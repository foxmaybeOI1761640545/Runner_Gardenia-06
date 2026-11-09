package de.dennisguse.opentracks.p0.app;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import java.util.List;
import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.databinding.P0HomeBinding;
import de.dennisguse.opentracks.chart.HeartRatePlot;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/** Debug-only native fixture host. No business connection, fixture values or release manifest entry. */
public final class DashboardPreviewActivity extends AbstractActivity {
 @Override protected void attachBaseContext(android.content.Context base){
  // A fixed comparison language belongs only to this Debug host. Night mode, density,
  // insets and font configuration still come from the actual Android device.
  var configuration=new android.content.res.Configuration(base.getResources().getConfiguration());configuration.setLocale(java.util.Locale.SIMPLIFIED_CHINESE);super.attachBaseContext(base.createConfigurationContext(configuration));
 }
 private P0HomeBinding binding;private DashboardRenderer renderer;
 @Override protected void onCreate(Bundle saved){super.onCreate(saved);renderer=new DashboardRenderer(this,binding);P0Navigation.bind(this,P0Activity.class);}
 public void present(DashboardState state,List<RouteGeometry.Point> route,List<HeartRatePlot.Point> heart){renderer.bind(state);binding.p0Title.setText(getString(de.dennisguse.opentracks.R.string.p04_brand)+" · UI fixture");binding.p04Route.bind(new RoutePreviewModel.Result(route,false,false),state.currentGps());binding.p03HeartChart.setPoints(heart);}
 @NonNull @Override protected View createRootView(){binding=P0HomeBinding.bind(getLayoutInflater().inflate(PreferencesUtils.getGardeniaTheme().homeLayout,null,false));return binding.getRoot();}
}
