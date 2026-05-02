package com.closetly.myapp.tags.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.closetly.myapp.tags.model.PredefinedTags
import com.closetly.myapp.tags.model.Tag

@Composable
fun TagChip(
    tag: Tag,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(tag.name) },
        modifier = modifier
    )
}

@Composable
fun TagSelector(
    selectedTagIds: List<String>,
    onTagToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TagGroup(
            label = "Jahreszeit",
            tags = PredefinedTags.SEASON,
            selectedTagIds = selectedTagIds,
            onTagToggle = onTagToggle
        )
        TagGroup(
            label = "Anlass",
            tags = PredefinedTags.OCCASION,
            selectedTagIds = selectedTagIds,
            onTagToggle = onTagToggle
        )
        TagGroup(
            label = "Stil",
            tags = PredefinedTags.STYLE,
            selectedTagIds = selectedTagIds,
            onTagToggle = onTagToggle
        )
    }
}

@Composable
private fun TagGroup(
    label: String,
    tags: List<Tag>,
    selectedTagIds: List<String>,
    onTagToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags) { tag ->
                TagChip(
                    tag = tag,
                    selected = tag.id in selectedTagIds,
                    onClick = { onTagToggle(tag.id) }
                )
            }
        }
    }
}
