package no.kantega.pdf;

import com.google.common.io.Files;
import no.kantega.pdf.conversion.IExternalConverter;
import no.kantega.pdf.conversion.msoffice.MicrosoftWordBridge;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractWordBasedTest extends AbstractWordAssertingTest {

    private static File EXTERNAL_CONVERTER_DIRECTORY;
    private static IExternalConverter EXTERNAL_CONVERTER;

    @BeforeClass
    public static void setUpConverter() throws Exception {
        EXTERNAL_CONVERTER_DIRECTORY = Files.createTempDir();
        EXTERNAL_CONVERTER = new MicrosoftWordBridge(EXTERNAL_CONVERTER_DIRECTORY,
                DEFAULT_CONVERSION_TIMEOUT, TimeUnit.MILLISECONDS);
        getWordAssert().assertWordRunning();
        assertTrue(EXTERNAL_CONVERTER.isOperational());
    }

    @AfterClass
    public static void tearDownConverter() throws Exception {
        try {
            EXTERNAL_CONVERTER.shutDown();
            assertFalse(EXTERNAL_CONVERTER.isOperational());
            getWordAssert().assertWordNotRunning();
        } finally {
            assertTrue(EXTERNAL_CONVERTER_DIRECTORY.delete());
        }
    }

    public static IExternalConverter getExternalConverter() {
        return EXTERNAL_CONVERTER;
    }

    private AtomicInteger nameGenerator;
    private File files;
    private Set<File> fileCopies;

    @Before
    public void setUpFiles() throws Exception {
        nameGenerator = new AtomicInteger(1);
        files = Files.createTempDir();
        fileCopies = Collections.newSetFromMap(new ConcurrentHashMap<File, Boolean>());
    }

    @After
    public void tearDownFiles() throws Exception {
        for (File copy : fileCopies) {
            assertTrue(copy.delete());
        }
        assertTrue(files.delete());
    }

    public File validDocx(boolean delete) throws IOException {
        return makeCopy(TestResource.DOCX_VALID, delete);
    }

    public File corruptDocx(boolean delete) throws IOException {
        return makeCopy(TestResource.DOCX_CORRUPT, delete);
    }

    public File inexistentDocx() throws IOException {
        return TestResource.DOCX_INEXISTENT.absoluteTo(files);
    }

    public File getFileFolder() {
        return files;
    }

    private File makeCopy(TestResource testResource, boolean delete) throws IOException {
        /*
         * When MS Word is asked to convert a file that is already opened by another program or by itself,
         * it will queue the conversion process until the file is released by the other process. This will cause
         * MS Word to be visible on the screen for a couple of milliseconds (screen flickering). On few occasions,
         * this will cause the conversion to fail. In practice, users should never use the converter on files
         * that are concurrently used by other applications or are currently converted by this application. Instead,
         * they should create a defensive copy before the conversion. In order to keep the tests stable, all tests
         * will however use a defensive copy.
         */
        File copy = testResource.materializeIn(files, String.format("%s.%d", testResource.getName(), nameGenerator.getAndIncrement()));
        assertTrue(copy.isFile());
        if (delete) {
            fileCopies.add(copy);
        }
        return copy;
    }

    public File makeTarget(boolean delete) {
        return makeTarget(String.format("target.%d.pdf", nameGenerator.getAndIncrement()), delete);
    }

    public File makeTarget(String name, boolean delete) {
        File target = new File(files, name);
        assertFalse(String.format("%s is not supposed to exist", target), target.exists());
        if (delete) {
            fileCopies.add(target);
        }
        return target;
    }
}