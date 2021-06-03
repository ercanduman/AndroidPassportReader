package example.jllarraz.com.passportreader

import androidx.multidex.MultiDexApplication

/**
 * Entry point of app.
 *
 * Created to remove static class declaration of androidx.multidex.MultiDexApplication in
 * AndroidManifest.xml file.
 *
 * @author ercanduman
 * @since  02.06.2021
 */
class MainApplication : MultiDexApplication()