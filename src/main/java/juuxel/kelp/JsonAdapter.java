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
