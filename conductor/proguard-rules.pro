# Retain constructor that is called by using reflection to recreate the Controller
-keepclassmembers public class * extends com.ivianuu.conductor.Controller {
   public <init>();
   public <init>(android.os.Bundle);
}