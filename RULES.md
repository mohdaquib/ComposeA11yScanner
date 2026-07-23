# Built-In Accessibility Rules

Compose A11y Scanner ships the built-in rules registered by `ScannerRules.allRuleIds()`.

Implementation note: the current codebase exposes 7 built-in rules, not 8. This document covers every implemented built-in rule in `scanner-rules`.

## Summary

| Rule ID | Name | Severity | What It Checks | How To Fix | WCAG Reference |
| --- | --- | --- | --- | --- | --- |
| `touch-target-overlap` | Touch Target Overlap | Warning | Interactive nodes whose effective Compose touch bounds overlap another effective target. | Increase spacing, enlarge layout bounds, or restructure controls so their effective hit regions do not overlap. | Android accessibility guidance |
| `missing-content-description` | Missing Content Description | Error | Interactive nodes and image-like nodes that do not expose a non-empty content description. | Add a meaningful `contentDescription` through semantics, or pass one directly to image composables that support it. | WCAG 1.1.1 Non-text Content (Level A) |
| `duplicate-content-description` | Duplicate Content Description | Warning | Non-merged nodes at the same semantics depth that reuse the same non-empty content description. | Give each control or item a label that identifies its specific action, state, or content. | WCAG 2.4.6 Headings and Labels (Level AA) |
| `focus-order` | Focus Order | Error | Focusable nodes whose semantics traversal jumps upward compared with the previous focusable node's visual position. | Reorder composables so focus follows the visual reading order, or set explicit traversal order with semantics. | WCAG 2.4.3 Focus Order (Level A) |
| `text-scaling` | Text Scaling | Warning | Text nodes that may overflow or clip inside their parent when simulated at a larger font scale. | Avoid fixed-height containers for text; use flexible height, wrapping, or scrolling so scaled text can reflow. | WCAG 1.4.4 Resize Text (Level AA) |
| `image-text-overlay` | Image With Text Overlay | Warning | Text nodes that significantly overlap image nodes, creating a contrast risk across dynamic images. | Add a scrim or solid text background, or otherwise guarantee sufficient contrast for every image state. | WCAG 1.4.3 Contrast Minimum (Level AA) |
| `clickable-role` | Clickable Role | Error | Clickable/touch target nodes that do not expose a semantic role, and clickable image roles without a content description. | Add the appropriate role, such as `Role.Button`, `Role.Checkbox`, or `Role.Image`; provide labels for clickable images. | WCAG 4.1.2 Name, Role, Value (Level A) |

## `touch-target-overlap` - Touch Target Overlap

**Severity:** Warning

**What it checks:** This scan-level rule compares `touchBoundsInRoot` for clickable nodes that are not merged descendants. It reports each affected node once when its effective pointer target intersects one or more other effective targets. Targets that only share an edge are not considered overlapping.

**How to fix:** Increase the layout spacing between controls, give controls layout bounds that accommodate their expanded hit regions, or restructure the layout so each action has an unambiguous pointer target.

**Reference:** Android accessibility touch-target guidance. This warning is not presented as a direct WCAG failure because WCAG target-size criteria include different thresholds and exceptions.

**Code example:**

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    IconButton(onClick = onPrevious) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
    }
    IconButton(onClick = onNext) {
        Icon(Icons.Default.ArrowForward, contentDescription = "Next")
    }
}
```

## `missing-content-description` - Missing Content Description

**Severity:** Error

**What it checks:** This rule reports interactive nodes and image-like composables, such as `Image` or `AsyncImage`, when they do not expose a non-empty content description. Merged descendants are skipped because their parent is responsible for the final announcement.

**How to fix:** Provide a meaningful label that describes the action or content. Decorative images should generally be excluded from semantics instead of receiving noisy labels.

**WCAG reference:** WCAG 1.1.1 Non-text Content (Level A)

**Code example:**

```kotlin
Icon(
    imageVector = Icons.Default.Search,
    contentDescription = "Search",
    modifier = Modifier.clickable(onClick = onSearch),
)
```

For a custom composable:

```kotlin
Box(
    modifier = Modifier.semantics {
        contentDescription = "Open account settings"
    },
)
```

## `duplicate-content-description` - Duplicate Content Description

**Severity:** Warning

**What it checks:** This rule groups non-merged nodes by semantics depth and content description. It reports nodes when more than one node at the same depth has the same non-empty content description.

**How to fix:** Make labels specific enough for a screen reader user to distinguish each item or action. Include item names, destinations, or state where needed.

**WCAG reference:** WCAG 2.4.6 Headings and Labels (Level AA)

**Code example:**

```kotlin
Row {
    IconButton(onClick = onEditProfile) {
        Icon(Icons.Default.Edit, contentDescription = "Edit profile")
    }

    IconButton(onClick = onEditAddress) {
        Icon(Icons.Default.Edit, contentDescription = "Edit address")
    }
}
```

## `focus-order` - Focus Order

**Severity:** Error

**What it checks:** This rule evaluates focusable nodes in semantics order and reports a node when focus moves upward by more than the configured threshold, 8dp by default. This catches cases where source order does not match the visible reading order.

**How to fix:** Prefer arranging composables in the same order users should navigate them. If the visual layout intentionally differs from source order, use semantics traversal ordering deliberately.

**WCAG reference:** WCAG 2.4.3 Focus Order (Level A)

**Code example:**

```kotlin
Column {
    TextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
    )

    TextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
    )

    Button(onClick = onSubmit) {
        Text("Sign in")
    }
}
```

When source order cannot match visual order:

```kotlin
Modifier.semantics {
    traversalIndex = 1f
}
```

## `text-scaling` - Text Scaling

**Severity:** Warning

**What it checks:** This rule looks for text nodes that may clip when their height is simulated at a larger font scale, 1.3x by default. It compares the scaled text bounds with the bounds of the nearest parent touch target.

**How to fix:** Avoid fixed heights around text. Use flexible containers, `wrapContentHeight()`, enough vertical padding, or scrolling so text can grow with the user's font-size preference.

**WCAG reference:** WCAG 1.4.4 Resize Text (Level AA)

**Code example:**

```kotlin
Button(
    onClick = onContinue,
    modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
) {
    Text(
        text = "Continue",
        modifier = Modifier.padding(vertical = 8.dp),
    )
}
```

## `image-text-overlay` - Image With Text Overlay

**Severity:** Warning

**What it checks:** This rule finds text nodes that overlap image nodes by more than the configured overlap threshold, 50% of the text area by default. It does not calculate exact contrast from every image state; it flags the pattern as a contrast risk.

**How to fix:** Place a scrim, gradient, or solid background behind the text, or move the text outside the image. Verify that normal text has at least 4.5:1 contrast and large text has at least 3:1 contrast.

**WCAG reference:** WCAG 1.4.3 Contrast Minimum (Level AA)

**Code example:**

```kotlin
Box {
    Image(
        painter = heroPainter,
        contentDescription = "Mountain trail",
        modifier = Modifier.fillMaxWidth(),
        contentScale = ContentScale.Crop,
    )

    Text(
        text = "Weekend hikes",
        color = Color.White,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .background(Color.Black.copy(alpha = 0.56f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
```

## `clickable-role` - Clickable Role

**Severity:** Error

**What it checks:** This rule reports clickable/touch target nodes that are not merged descendants and do not expose a semantic role. It also reports clickable image roles when the content description is empty.

**How to fix:** Use Material components when possible because they usually provide roles automatically. For custom click targets, set the appropriate role through semantics or use clickable APIs that expose the role.

**WCAG reference:** WCAG 4.1.2 Name, Role, Value (Level A)

**Code example:**

```kotlin
Box(
    modifier = Modifier
        .semantics {
            role = Role.Button
            contentDescription = "Retry loading"
        }
        .clickable(onClick = onRetry)
        .minimumInteractiveComponentSize(),
) {
    Text("Retry")
}
```
