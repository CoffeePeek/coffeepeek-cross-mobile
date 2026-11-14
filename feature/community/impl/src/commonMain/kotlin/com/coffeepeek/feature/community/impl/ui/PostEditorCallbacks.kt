package com.coffeepeek.feature.community.impl.ui

import com.coffeepeek.feature.community.domain.model.PostRating
import com.coffeepeek.feature.community.impl.timeline.CommunityAction
import com.coffeepeek.feature.community.impl.post.PostAction

internal class PostEditorCallbacks(
    val onText: (String) -> Unit, val onRating: (PostRating) -> Unit,
    val onDrink: (String?, String?) -> Unit, val onReloadDrinks: () -> Unit,
    val onSave: () -> Unit, val onDismiss: () -> Unit,
)

internal fun timelineEditorCallbacks(action: (CommunityAction) -> Unit) = PostEditorCallbacks(
    { action(CommunityAction.EditText(it)) }, { action(CommunityAction.EditRating(it)) },
    { slug, name -> action(CommunityAction.EditDrink(slug, name)) },
    { action(CommunityAction.LoadDrinks) }, { action(CommunityAction.SaveEdit) }, { action(CommunityAction.DismissEdit) },
)

internal fun postEditorCallbacks(action: (PostAction) -> Unit) = PostEditorCallbacks(
    { action(PostAction.EditText(it)) }, { action(PostAction.EditRating(it)) },
    { slug, name -> action(PostAction.EditDrink(slug, name)) },
    { action(PostAction.LoadDrinks) }, { action(PostAction.SaveEdit) }, { action(PostAction.DismissEdit) },
)
