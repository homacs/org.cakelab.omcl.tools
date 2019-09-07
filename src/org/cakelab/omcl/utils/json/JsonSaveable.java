package org.cakelab.omcl.utils.json;

import java.io.File;
import java.io.IOException;

import org.cakelab.json.codec.JSONCodecException;

public interface JsonSaveable {
	void save(File f)  throws IOException, JSONCodecException;
}
