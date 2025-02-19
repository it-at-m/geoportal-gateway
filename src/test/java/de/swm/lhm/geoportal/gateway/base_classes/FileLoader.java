package de.swm.lhm.geoportal.gateway.base_classes;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public abstract class FileLoader {

    protected String loadFileContent(String name) throws IOException {

        URL resource = getClass().getResource(name);

        if (resource == null)
            resource = new ClassPathResource(name, getClass().getClassLoader()).getURL();

        File file = new File(resource.getFile());
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    protected FileInputStream getFileAsStream(String name) throws IOException {

        URL resource = getClass().getResource(name);

        if (resource == null)
            resource = new ClassPathResource(name, getClass().getClassLoader()).getURL();

        return new FileInputStream(resource.getFile());

    }
}
