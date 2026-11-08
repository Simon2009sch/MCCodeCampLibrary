# Custom item routing

`ItemsManager` owns its listener registration and must be closed on plugin disable or replacement. `close()` is idempotent, unregisters all its listeners, clears handlers and paired-click state, and prevents new registration. `unregisterCustomItem(ACustomItemHandler)` removes only the exact registered instance; prefer this overload in fixture cleanup. Duplicate registration retains the original handler. The String overload removes the current handler by ID.

## Hooks (all synchronous, cancelled before callback)

- `onInteract(PlayerInteractEvent)` defaults to no-op and handles left/right air clicks.
- `onBlockInteract(PlayerInteractEvent)` handles left/right block clicks; its default delegates to `onInteract` exactly once, retaining legacy wand behavior.
- `onEntityInteract(PlayerInteractEntityEvent)` handles right-clicks using the event's hand. The argument can be `PlayerInteractAtEntityEvent` (including precise-only armor-stand interactions).
- `onEntityAttack(PrePlayerAttackEntityEvent)` handles left-click entities with the main hand before damage, including entities which will not be attacked. Listeners deliberately do not ignore already-cancelled events. Paper keeps `willAttack() == false` events cancelled even if a callback attempts to un-cancel.

Callbacks may un-cancel their click event to opt back into vanilla behavior. Ordinary items, unknown/unregistered IDs, foreign PDC types, malformed JSON and non-string JSON IDs are not intercepted. Physical actions are not tool clicks. A separate `BlockBreakEvent` listener always cancels breaking while a registered custom item is held in the main hand; it does not invoke a second click callback. This protection is intentionally independent of click un-cancellation.

### Right-click packet pairing

The resolved Paper 1.21.11 API source documents that clients can send both generic and precise entity packets, and precise events have their own HandlerList. Both event types therefore need listeners. The manager pairs opposite forms in **the same server tick**, keyed by player UUID, entity UUID, hand and registered handler identity. The first event invokes the hook; the second mirrors its final cancellation without a second callback. A completed pair is consumed, repeated same-form events still invoke callbacks, and different hands/targets/players are independent. State expires on the next custom entity interaction in a different tick, and is cleared on unregister/close. Callback-driven close/unregister cannot repopulate the state.

Bukkit exposes no gesture/packet correlation identifier: this is explicitly a same-tick pairing policy, not a guarantee for a client spreading duplicate packet forms over different ticks; conversely two independent opposite-form interactions in one tick are indistinguishable from a pair. The hook receives whichever form arrives first, not necessarily the precise coordinates.

## Persistence

The existing `ITEM_KEY` and root `id`, `dataVersion`, `itemData` format are unchanged. Unknown `createNewItem(String)` IDs return null. Protected `setItemData` changes only `itemData`, preserving ID/version and future root metadata; it rejects stacks without a valid configuration rather than generating an unregistered tag.

## Verification

From the repository root:

```sh
python MCCodeCampLibrary/tests/run_custom_items.py
```

The runner compiles the **actual two production classes**, runs ten scenario groups, and uses real Bukkit event/configuration classes with Player/inventory/PDC/PluginManager boundary doubles and a minimal ItemStack double. A facade double provides only logging so concurrent Maven cleans do not break this isolated harness. It exercises behavior, not source-string assertions. Each new behavior was introduced through a failing harness run before its implementation. The runner accepts individual scenario names: `unknown block entity attack break hands malformed metadata lifecycle reentrant`.

This proves routing, cancellation, hand selection, data preservation and ownership cleanup at the API boundary, not actual Paper packet ordering or in-game block/client behavior. Verify live entity packet pairing, armor stands, invulnerable entities and held-tool break prevention on a test server before deployment.
