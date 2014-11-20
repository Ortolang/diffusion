package fr.ortolang.diffusion;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import fr.ortolang.diffusion.api.OrtolangObjectidentifierTest;
import fr.ortolang.diffusion.core.CollectionUnitTest;
import fr.ortolang.diffusion.core.CoreServiceTest;
import fr.ortolang.diffusion.core.PathBuilderTest;
import fr.ortolang.diffusion.registry.RegistryServiceTest;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceTest;
import fr.ortolang.diffusion.store.index.IndexStoreServiceTest;
import fr.ortolang.diffusion.store.triple.TripleStoreServiceTest;


@RunWith(Suite.class)
@SuiteClasses({ 
	OrtolangObjectidentifierTest.class,
	CollectionUnitTest.class,
	CoreServiceTest.class,
	PathBuilderTest.class,
	RegistryServiceTest.class,
	BinaryStoreServiceTest.class,
//	HandleStoreServiceTest.class,
	IndexStoreServiceTest.class,
	TripleStoreServiceTest.class
})
public class DiffusionTestSuite {

}
