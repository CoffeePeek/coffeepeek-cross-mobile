package com.coffeepeek.admin.releasecheck;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.coffeepeek.admin.feature.checkin.data.ReleaseDrinkPhotoProbe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;

/** Platform-only smoke runner. Loaded only by the opt-in local release-test build. */
public final class DrinkPhotoReleaseInstrumentation extends Instrumentation {
    private static final String SUITE = "com.coffeepeek.admin.releasecheck.DrinkPhotoReleaseSmoke";
    private static final byte[] PNG_SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    private Bundle arguments;

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        this.arguments = arguments == null ? new Bundle() : new Bundle(arguments);
        start();
    }

    @Override
    public void onStart() {
        super.onStart();
        long started = SystemClock.elapsedRealtime();
        String privatePath = arguments.getString("cupInHandFixture");
        boolean hasPrivateFixture = privatePath != null && !privatePath.trim().isEmpty();
        int count = hasPrivateFixture ? 2 : 1;
        int failures = runTest("bundledModelsProduceTransparentCupPng", 1, count, () -> {
            waitForIdleSync(); // Application.onCreate must finish before the extractor uses its context.
            byte[] source;
            try (InputStream input = getContext().getAssets().open("checkin/coffee.jpg")) {
                source = readAll(input);
            }
            byte[][] cutouts = extractAndValidate(source);
            require(cutouts.length == 1, "Expected one cup from the CC0 fixture");
            Bitmap cup = decode(cutouts[0]);
            try {
                require(cup.getWidth() >= 180 && cup.getHeight() >= 180, "The whole cup must be retained");
                int[] pixels = pixels(cup);
                int porcelainPixels = 0;
                for (int pixel : pixels) {
                    if (Color.alpha(pixel) > 240 && Color.red(pixel) > 190 && Color.green(pixel) > 190
                            && Color.blue(pixel) > 170 && Math.abs(Color.red(pixel) - Color.blue(pixel)) < 45) {
                        porcelainPixels++;
                    }
                }
                require(porcelainPixels > 100, "The cutout must contain the pale ceramic cup");
            } finally {
                cup.recycle();
            }
        });
        if (hasPrivateFixture) {
            failures += runTest("suppliedCupInHandKeepsTheCupAndRemovesTheHand", 2, count, () -> {
                waitForIdleSync();
                byte[] source;
                try (InputStream input = new FileInputStream(privatePath)) {
                    source = readAll(input);
                }
                byte[][] cutouts = extractAndValidate(source);
                require(cutouts.length == 1, "Expected one cup from the private fixture");
                File external = getTargetContext().getExternalFilesDir(null);
                require(external != null, "Target external files directory is unavailable");
                File folder = new File(external, "cup-result");
                require(folder.isDirectory() || folder.mkdirs(), "Cannot create cutout output directory");
                try (FileOutputStream output = new FileOutputStream(new File(folder, "cup-without-hand.png"))) {
                    output.write(cutouts[0]);
                }
                Bitmap cup = decode(cutouts[0]);
                try {
                    float ratio = (float) cup.getHeight() / cup.getWidth();
                    require(ratio >= 0.8f && ratio <= 1.4f, "Hand/arm must not extend the cup bounds");
                    int x = (int) (cup.getWidth() * 0.58f);
                    int y = (int) (cup.getHeight() * 0.94f);
                    require(Color.alpha(cup.getPixel(x, y)) == 0, "Thumb below the printed cup must be transparent");
                    require(Color.alpha(cup.getPixel(cup.getWidth() / 2, cup.getHeight() / 2)) == 255,
                            "Cup body must remain visible");
                } finally {
                    cup.recycle();
                }
            });
        }
        Bundle result = new Bundle();
        result.putInt("numtests", count);
        String summary = failures == 0
                ? "OK (" + count + " tests)"
                : "FAILURES!!!\nTests run: " + count + ", Failures: " + failures;
        result.putString("stream", "\nTime: " + ((SystemClock.elapsedRealtime() - started) / 1000.0) + "\n\n" + summary + "\n");
        finish(-1, result);
    }

    private int runTest(String name, int current, int total, CheckedTest test) {
        sendStatus(1, status(name, current, total));
        try {
            test.run();
            Bundle passed = status(name, current, total);
            passed.putString("stream", ".");
            sendStatus(0, passed);
            return 0;
        } catch (Throwable failure) {
            Bundle failed = status(name, current, total);
            String stack = Log.getStackTraceString(failure);
            failed.putString("stack", stack);
            failed.putString("stream", "\nFailure in " + name + ":\n" + stack);
            sendStatus(-2, failed);
            return 1;
        }
    }

    private Bundle status(String name, int current, int total) {
        Bundle status = new Bundle();
        status.putString("id", "InstrumentationTestRunner");
        status.putString("class", SUITE);
        status.putString("test", name);
        status.putInt("numtests", total);
        status.putInt("current", current);
        return status;
    }

    private byte[][] extractAndValidate(byte[] source) {
        byte[] snapshot = source.clone();
        byte[][] cutouts = ReleaseDrinkPhotoProbe.extract(source);
        require(Arrays.equals(snapshot, source), "Extraction must not mutate the original photo");
        require(cutouts != null && cutouts.length > 0, "Extraction returned no cup cutouts");
        for (byte[] cutout : cutouts) {
            require(cutout != null && cutout.length >= PNG_SIGNATURE.length && cutout.length <= 4_194_304,
                    "Cutout size must fit the PNG upload contract");
            for (int index = 0; index < PNG_SIGNATURE.length; index++) {
                require(cutout[index] == PNG_SIGNATURE[index], "Cutout must be a PNG");
            }
            Bitmap image = decode(cutout);
            try {
                require(image.getWidth() > 0 && image.getWidth() <= 768
                                && image.getHeight() > 0 && image.getHeight() <= 768,
                        "Cutout dimensions must be between 1 and 768 pixels");
                require(image.hasAlpha(), "Cutout must have an alpha channel");
                boolean transparent = false;
                boolean visible = false;
                for (int pixel : pixels(image)) {
                    transparent |= Color.alpha(pixel) < 255;
                    visible |= Color.alpha(pixel) > 0;
                }
                require(transparent && visible, "Cutout must contain transparent and visible pixels");
            } finally {
                image.recycle();
            }
        }
        return cutouts;
    }

    private static Bitmap decode(byte[] bytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        require(bitmap != null, "PNG must be decodable");
        return bitmap;
    }

    private static int[] pixels(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        return pixels;
    }

    private static byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            bytes.write(buffer, 0, read);
        }
        return bytes.toByteArray();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private interface CheckedTest {
        void run() throws Exception;
    }
}
