package hudson.plugins.scm_sync_configuration.basic;

import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationBaseTest;
import hudson.plugins.test.utils.scms.ScmUnderTestSubversion;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ScmSyncConfigurationBasicTest extends ScmSyncConfigurationBaseTest {

    public ScmSyncConfigurationBasicTest() {
        super(new ScmUnderTestSubversion());
    }

    @Test
    public void shouldRetrieveMockedHudsonInstanceCorrectly() throws Throwable {
        Hudson hudsonInstance = Hudson.getInstance();
        assertThat(hudsonInstance, is(notNullValue()));
        assertThat(hudsonInstance.toString().split("@")[0], is(not(equalTo("hudson.model.Hudson"))));
    }

    @Test
    public void shouldVerifyIfHudsonRootDirectoryExists() throws Throwable {

        Hudson hudsonInstance = Hudson.getInstance();
        File hudsonRootDir = hudsonInstance.getRootDir();
        assertThat(hudsonRootDir, is(not(equalTo(null))));
        assertThat(hudsonRootDir.exists(), is(true));
    }
}
