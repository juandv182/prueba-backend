package fastglp.utils;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class KeyPair <K extends Comparable<K>> implements Serializable {
    private K key1;
    private K key2;

    public KeyPair(K key1, K key2) {
        // Always store the smaller key first to ensure uniqueness
        if (key1.compareTo(key2) < 0) {
            this.key1 = key1;
            this.key2 = key2;
        } else {
            this.key1 = key2;
            this.key2 = key1;
        }
    }

    public K key1() {
        return key1;
    }

    public K key2() {
        return key2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (KeyPair) obj;
        return Objects.equals(this.key1, that.key1) &&
                Objects.equals(this.key2, that.key2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key1, key2);
    }

    @Override
    public String toString() {
        return "[" +
                key1 + ", " +
                key2 + ']';
    }
}
