/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.kelp;

public interface JsonAdapter<T, D> {
    T fromJson(D data);
    D toJson(T value);

    abstract class ReadOnly<T, D> implements JsonAdapter<T, D> {
        protected final <R> R unsupported() {
            throw new UnsupportedOperationException("Writing is not supported!");
        }
    }
}
