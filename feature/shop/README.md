# Shop feature — gallery and detail preparation

`feature/shop` owns shop browsing. The first Android slice migrates only the
menu-photo gallery; the much larger shop-details screen stays in legacy code.
The next preparation slice adds a read-only `ShopOverview` contract and mapper
for the detail header, without switching that screen yet.
The existing shared/iOS route and ViewModel remain untouched.

| Module | Responsibility | Allowed dependencies and consumers |
|---|---|---|
| `api` | Minimal composable gallery entry and caller callbacks | Compose runtime; application composition |
| `domain` | `MenuGallery`, `MenuPhoto`, read-only `ShopOverview`, and repository contracts returning `Result` | Pure Kotlin; shop data/impl |
| `data` | Shared shop-details HTTP request, narrow DTOs, mappers and factories | Domain, core/network, Ktor, serialization; application composition |
| `impl` | MVI gallery screen, resources, previews and API adapter | API/domain, core presentation/design-system; application composition |

Packages alone cannot enforce the domain/transport/UI boundaries or keep HTTP
types out of the public feature entry. The application passes its already
configured authenticated `HttpClient` to the data factory. No second client or
feature Koin module is created. Gallery and overview data are read from the
same `GET /api/CoffeeShops/{id}` endpoint as legacy; DTOs decode only the
fields those two slices need. Both use one `ShopDetailsBackend`, not a
parallel HTTP endpoint. The menu mapper keeps the legacy preference for
`urls.fullscreen`/`urls.detail`, falls back to `fullUrl`, drops missing URLs and
sorts by `sortIndex`. A top-level menu is used when `shopDto.menu` is absent.

Android composition renders the new entry and adapts its photo-open callback
to the existing full-screen viewer, preserving swipe/zoom behaviour without
adding a feature-to-app dependency or copying that shared component. The
root back stack remains application-owned. Feature UI does not see DTOs, Ktor,
legacy repositories or Koin.

## Migration sequence and risks

1. Add domain/data/API/UI contracts and cover result, mapper and MVI behaviour.
2. Switch only the Android gallery route via a platform renderer; keep the
   legacy shared/iOS renderer as the default.
3. Compile the app and inspect light/dark previews and gallery behaviour using
   a non-production endpoint. A successful build does not prove server data or
   image loading at runtime.
4. Add stateless detail UI and its MVI coordination in independent slices,
   including favorites, check-ins, reviews and navigation, before switching the
   Android detail route. Do not show a reduced read-only replacement meanwhile.
5. Later migrate the rest of shop browsing under this same owner. Retire legacy
   DTOs, repositories and screen paths only after all platform consumers move.

Potential risks: backend DTO shape varies between nested and top-level menu;
images may be missing or URLs may expire; iOS still relies on legacy source.
The feature has only default-language resources until translations are audited.
