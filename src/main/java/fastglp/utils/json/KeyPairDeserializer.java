package fastglp.utils.json;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import fastglp.model.Coordenada;
import fastglp.utils.KeyPair;

import java.io.IOException;

public class KeyPairDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        // Remove the "KeyPair" and brackets from the string
        key = key.replace("[(", "").replace("), (", ",").replace(")]", "");
        String[] parts = key.split(",");

        // Parse the coordinates
        Coordenada key1 = new Coordenada(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
        Coordenada key2 = new Coordenada(Double.parseDouble(parts[2].trim()), Double.parseDouble(parts[3].trim()));

        return new KeyPair<>(key1, key2);
    }
}
