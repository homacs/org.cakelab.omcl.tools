package org.cakelab.omcl.utils.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.cakelab.omcl.utils.FileSystem;


public class ManifestAccess {

	public static Manifest read(File path) throws IOException {
		String extension = FileSystem.getFileNameExtension(path);
		Manifest mf;
		if (extension.equals("jar")) {
			Jar jar = new Jar(path);
			mf = jar.readManifest();
		} else {
			mf = new Manifest(new FileInputStream(path));
		}
		return mf;
		
	}

	public static void add(Manifest src, Manifest trg) {
		for (Map.Entry<String, Attributes> entry : src.getEntries().entrySet()) {
			Attributes trgAttr = trg.getAttributes(entry.getKey());
			if (trgAttr == null) {
				trg.getEntries().put(entry.getKey(), entry.getValue());
			} else {
				trgAttr.putAll(entry.getValue());
			}
		}
	}

	/**
	 * Writes the given manifest to the given file or java archive if requested.
	 * Note: Also it is supported it is not advised to write the manifest 
	 *       directly to a jar file because the archive will be unpacked and packed
	 *       in this procedure which might cause more latency than expected.
	 * 
	 * @param mf
	 * @param path
	 * @throws IOException
	 */
	public static void write(Manifest mf, File path) throws IOException {
		String extension = FileSystem.getFileNameExtension(path);
		if (extension.equals("jar")) {
			Jar jar = new Jar(path);
			jar.writeManifest(mf);
		} else {
			mf.write(new FileOutputStream(path));
		}
	}

}
