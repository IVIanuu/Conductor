//@file:Suppress("ClassName", "unused")

object Versions {
    // android
    const val compileSdk = 27
    const val minSdk = 16
    const val targetSdk = 27
    const val versionCode = 1
    const val versionName = "1.0"

    const val androidGradlePlugin = "3.1.2"
    const val archComponents = "1.1.1"
    const val autoDispose = "6f10c435c1"
    const val constraintLayout = "1.1.0"
    const val fabric = "2.7.1"
    const val kotlin = "1.2.41"
    const val ktx = "0.3"
    const val mavenGradle = "2.1"
    const val rxActivityResult = "6da49c1fcf"
    const val rxAndroid = "2.0.2"
    const val rxJava = "2.1.13"
    const val rxKotlin = "2.2.0"
    const val rxPermissions = "981f695508"
    const val rxPreferences = "2.0.0"
    const val support = "27.1.1"
}
//
object Deps {
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"

    const val archComponentsLifecycleExtensions = "android.arch.lifecycle:extensions:${Versions.archComponents}"

    const val autoDispose = "com.github.IVIanuu.AutoDispose:autodispose:${Versions.autoDispose}"
    const val autoDisposeView = "com.github.IVIanuu.AutoDispose:autodispose-view:${Versions.autoDispose}"

    const val constraintLayout = "com.android.support.constraint:constraint-layout:${Versions.constraintLayout}"

    const val fabric = "com.crashlytics.sdk.android:crashlytics:${Versions.fabric}@aar"

    const val ktx = "androidx.core:core-ktx:${Versions.ktx}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jre7:${Versions.kotlin}"

    const val mavenGradlePlugin = "com.github.dcendents:android-maven-gradle-plugin:${Versions.mavenGradle}"

    const val rxActivityResult = "com.github.IVIanuu:RxActivityResult:${Versions.rxActivityResult}"
    const val rxAndroid = "io.reactivex.rxjava2:rxandroid:${Versions.rxAndroid}"
    const val rxJava = "io.reactivex.rxjava2:rxjava:${Versions.rxJava}"
    const val rxKotlin = "io.reactivex.rxjava2:rxkotlin:${Versions.rxKotlin}"
    const val rxPermissions = "com.github.IVIanuu:RxPermissions:${Versions.rxPermissions}"
    const val rxPreferences = "com.f2prateek.rx.preferences2:rx-preferences:${Versions.rxPreferences}"

    const val supportAppCompat = "com.android.support:appcompat-v7:${Versions.support}"
    const val supportDesign = "com.android.support:design:${Versions.support}"
    const val supportFragments = "com.android.support:support-fragment:${Versions.support}"
    const val supportRecyclerView = "com.android.support:recyclerview-v7:${Versions.support}"
}