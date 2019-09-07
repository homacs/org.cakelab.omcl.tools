package org.cakelab.omcl.plugins.forge;

import java.io.File;

import org.cakelab.omcl.plugins.ServicesStubBase;
import org.cakelab.omcl.plugins.StubException;

public abstract class ForgeServicesStub extends ServicesStubBase {

	public ForgeServicesStub(ClassLoader classLoader) {
		super(classLoader);
	}


	
	public abstract boolean installClient(File workDir) throws StubException;
	
}
