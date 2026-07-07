import { useSyncExternalStore } from "react";

const subscribe = () => () => {};

/**
 * True once hydrated on the client, false during SSR. Uses
 * useSyncExternalStore (server/client snapshots differ) instead of an
 * effect + setState, which the React Compiler lint rules disallow.
 */
export function useHasMounted() {
  return useSyncExternalStore(subscribe, () => true, () => false);
}
