/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class CartesianProduct {
    public static <T> List<List<T>> of(List<List<T>> values) {
        List<List<T>> out = new ArrayList<>();
        createRecursive(List.of(), values, out::add);
        return out;
    }

    private static <T> void createRecursive(List<T> head, List<List<T>> values, Consumer<List<T>> out) {
        var toAdd = values.getFirst();
        for (T t : toAdd) {
            var list = new ArrayList<>(head);
            list.add(t);

            if (values.size() == 1) {
                out.accept(list);
            } else {
                createRecursive(list, values.subList(1, values.size()), out);
            }
        }
    }
}
