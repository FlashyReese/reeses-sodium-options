package me.flashyreese.mods.reeses_sodium_options.client.gui.state;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class SearchResultOrdering {
    /**
     * Orders {@code items} (supplied in page-display order) by the search {@code resultIds}.
     * {@link SearchResultOrder#PAGE_DISPLAY} keeps page order filtered to the result set;
     * {@link SearchResultOrder#RANKED} follows the order of {@code resultIds}.
     */
    public static <T> List<T> order(SearchResultOrder order, List<Identifier> resultIds, List<T> items, Function<T, Identifier> idOf) {
        if (resultIds.isEmpty()) {
            return List.of();
        }

        return switch (order) {
            case PAGE_DISPLAY -> {
                Set<Identifier> resultIdSet = new HashSet<>(resultIds);
                List<T> ordered = new ArrayList<>(resultIds.size());
                for (T item : items) {
                    if (resultIdSet.contains(idOf.apply(item))) {
                        ordered.add(item);
                    }
                }
                yield ordered;
            }
            case RANKED -> {
                Map<Identifier, T> itemsById = new HashMap<>();
                for (T item : items) {
                    itemsById.putIfAbsent(idOf.apply(item), item);
                }
                List<T> ordered = new ArrayList<>(resultIds.size());
                for (Identifier id : resultIds) {
                    T item = itemsById.get(id);
                    if (item != null) {
                        ordered.add(item);
                    }
                }
                yield ordered;
            }
        };
    }
}
