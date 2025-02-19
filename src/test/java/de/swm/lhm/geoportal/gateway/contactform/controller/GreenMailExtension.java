package de.swm.lhm.geoportal.gateway.contactform.controller;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailProxy;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

// greenmail-junit5 provides this class but the package still use an old greenmail core version
// which uses javax instead of jakarta
// ToDo check if newer version of greenmail-junit5 with a greenmal-core > 2.0.0 version is available

public class GreenMailExtension extends GreenMailProxy implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    private final ServerSetup[] serverSetups;
    private GreenMail greenMail;
    private boolean perMethod;

    public GreenMailExtension(ServerSetup serverSetup) {
        this.perMethod = true;
        this.serverSetups = new ServerSetup[]{serverSetup};
    }

    public GreenMailExtension() {
        this(ServerSetupTest.ALL);
    }

    public GreenMailExtension(ServerSetup[] serverSetups) {
        this.perMethod = true;
        this.serverSetups = serverSetups;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (this.perMethod) {
            this.greenMail = new GreenMail(this.serverSetups);
            this.start();
        }

    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (this.perMethod) {
            this.stop();
        }

    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!this.perMethod) {
            this.greenMail = new GreenMail(this.serverSetups);
            this.start();
        }

    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (!this.perMethod) {
            this.stop();
        }

    }

    public GreenMailExtension withPerMethodLifecycle(boolean perMethod) {
        this.perMethod = perMethod;
        return this;
    }

    @Override
    protected GreenMail getGreenMail() {
        return this.greenMail;
    }

    @Override
    public GreenMailExtension withConfiguration(GreenMailConfiguration config) {
        super.withConfiguration(config);
        return this;
    }

    @Override
    public boolean isRunning() {
        return this.greenMail.isRunning();
    }
}
