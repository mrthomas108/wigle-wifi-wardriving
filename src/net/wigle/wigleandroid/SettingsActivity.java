package net.wigle.wigleandroid;

import java.util.Arrays;

import net.wigle.wigleandroid.MainActivity.Doer;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * configure settings
 */
public final class SettingsActivity extends Activity {
  
  private static final int MENU_RETURN = 12;
  private static final int MENU_ERROR_REPORT = 13;
  
  /** convenience, just get the darn new string */
  public static abstract class SetWatcher implements TextWatcher {
    public void afterTextChanged( final Editable s ) {}
    public void beforeTextChanged( final CharSequence s, final int start, final int count, final int after ) {}
    public void onTextChanged( final CharSequence s, final int start, final int before, final int count ) {
      onTextChanged( s.toString() ); 
    }
    public abstract void onTextChanged( String s );
  }
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate( final Bundle savedInstanceState) {
      super.onCreate( savedInstanceState );
      // set language
      MainActivity.setLocale( this );
      setContentView( R.layout.settings );
      
      // force media volume controls
      this.setVolumeControlStream( AudioManager.STREAM_MUSIC );

      // don't let the textbox have focus to start with, so we don't see a keyboard right away
      final LinearLayout linearLayout = (LinearLayout) findViewById( R.id.linearlayout );
      linearLayout.setFocusableInTouchMode(true);
      linearLayout.requestFocus();
      
      // get prefs
      final SharedPreferences prefs = this.getSharedPreferences( ListActivity.SHARED_PREFS, 0);
      final Editor editor = prefs.edit();
      
      // donate
      final CheckBox donate = (CheckBox) findViewById(R.id.donate);
      final boolean isDonate = prefs.getBoolean( ListActivity.PREF_DONATE, false);
      
      donate.setChecked( isDonate );
      if ( isDonate ) {
        eraseDonate();
      }
      donate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
          public void onCheckedChanged( final CompoundButton buttonView, final boolean isChecked ) { 
            if ( isChecked == prefs.getBoolean( ListActivity.PREF_DONATE, false) ) {
              // this would cause no change, bail
              return;
            }
            
            if ( isChecked ) {
              // turn off until confirmed
              buttonView.setChecked( false );
              // confirm
              MainActivity.createConfirmation( SettingsActivity.this, 
                  "Donate data to WiGLE?\n\n"
                  + "Allow WiGLE to make an anonymized copy of data which WiGLE is authorized to license commercially to other parties.", 
                  new Doer() {
                @Override
                public void execute() {
                  editor.putBoolean( ListActivity.PREF_DONATE, isChecked );
                  editor.commit();
                                    
                  buttonView.setChecked( true );
                  // poof
                  eraseDonate();
                }
              });
            }
            else {
              editor.putBoolean( ListActivity.PREF_DONATE, isChecked );
              editor.commit();
            }
          }
        });
      
      // anonymous
      final CheckBox beAnonymous = (CheckBox) findViewById(R.id.be_anonymous);
      final EditText user = (EditText) findViewById(R.id.edit_username);
      final EditText pass = (EditText) findViewById(R.id.edit_password);
      final boolean isAnonymous = prefs.getBoolean( ListActivity.PREF_BE_ANONYMOUS, false);
      if ( isAnonymous ) {
        user.setEnabled( false );
        pass.setEnabled( false );
      }
      
      beAnonymous.setChecked( isAnonymous );
      beAnonymous.setOnCheckedChangeListener(new OnCheckedChangeListener() {
          public void onCheckedChanged( final CompoundButton buttonView, final boolean isChecked ) { 
            if ( isChecked == prefs.getBoolean( ListActivity.PREF_BE_ANONYMOUS, false) ) {
              // this would cause no change, bail
              return;
            }
            
            if ( isChecked ) {
              // turn off until confirmed
              buttonView.setChecked( false );
              // confirm
              MainActivity.createConfirmation( SettingsActivity.this, "Upload anonymously?", new Doer() {
                @Override
                public void execute() {
                  // turn anonymous
                  user.setEnabled( false );
                  pass.setEnabled( false );
                  
                  editor.putBoolean( ListActivity.PREF_BE_ANONYMOUS, isChecked );
                  editor.commit();
                  
                  buttonView.setChecked( true );
                  
                  // might have to remove or show register link
                  updateRegister();
                }
              });
            }
            else {
              // unset anonymous
              user.setEnabled( true );
              pass.setEnabled( true );
              
              editor.putBoolean( ListActivity.PREF_BE_ANONYMOUS, isChecked );
              editor.commit();
              
              // might have to remove or show register link
              updateRegister();
            }            
          }
        });
      
      // register link
      final TextView register = (TextView) findViewById(R.id.register);
      register.setText(Html.fromHtml("<a href='http://wigle.net/gps/gps/main/register'>Register</a>"
          + " at <a href='http://wigle.net/gps/gps/main/register'>WiGLE.net</a>"));
      register.setMovementMethod(LinkMovementMethod.getInstance());
      updateRegister();
            
      user.setText( prefs.getString( ListActivity.PREF_USERNAME, "" ) );
      user.addTextChangedListener( new SetWatcher() {
        public void onTextChanged( final String s ) {
          // ListActivity.debug("user: " + s);
          editor.putString( ListActivity.PREF_USERNAME, s.trim() );
          editor.commit();
          // might have to remove or show register link
          updateRegister();
        } 
      });
      
      final CheckBox showPassword = (CheckBox) findViewById(R.id.showpassword);
      showPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        public void onCheckedChanged( final CompoundButton buttonView, final boolean isChecked ) { 
          if ( isChecked ) {
            pass.setTransformationMethod(SingleLineTransformationMethod.getInstance());
          }
          else {
            pass.setTransformationMethod(PasswordTransformationMethod.getInstance());
          }
        }
      });
      
      pass.setText( prefs.getString( ListActivity.PREF_PASSWORD, "" ) );
      pass.addTextChangedListener( new SetWatcher() {
        public void onTextChanged( final String s ) {
          // ListActivity.debug("pass: " + s);
          editor.putString( ListActivity.PREF_PASSWORD, s.trim() );
          editor.commit();
        } 
      });
      
      final Button button = (Button) findViewById( R.id.speech_button );
      button.setOnClickListener( new OnClickListener() {
          public void onClick( final View view ) {
            final Intent errorReportIntent = new Intent( SettingsActivity.this, SpeechActivity.class );
            SettingsActivity.this.startActivity( errorReportIntent );
          }
        });
            
      // db marker reset button and text
      final TextView tv = (TextView) findViewById( R.id.reset_maxid_text );
      tv.setText( getString(R.string.setting_high_up) + " " + prefs.getLong( ListActivity.PREF_DB_MARKER, 0L ) );
      
      final Button resetMaxidButton = (Button) findViewById( R.id.reset_maxid_button );
      resetMaxidButton.setOnClickListener( new OnClickListener() {
        public void onClick( final View buttonView ) {    
          MainActivity.createConfirmation( SettingsActivity.this, getString(R.string.setting_zero_out), new Doer() {
            @Override
            public void execute() {          
              editor.putLong( ListActivity.PREF_DB_MARKER, 0L );
              editor.commit();
              tv.setText( getString(R.string.setting_max_id) + " 0" );
            }
          } );
        }
      });
      
      // db marker maxout button and text
      final TextView maxtv = (TextView) findViewById( R.id.maxout_maxid_text );
      final long maxDB = prefs.getLong( ListActivity.PREF_MAX_DB, 0L );
      maxtv.setText( getString(R.string.setting_max_start) + " " + maxDB );
      
      final Button maxoutMaxidButton = (Button) findViewById( R.id.maxout_maxid_button );
      maxoutMaxidButton.setOnClickListener( new OnClickListener() {
        public void onClick( final View buttonView ) { 
          MainActivity.createConfirmation( SettingsActivity.this, getString(R.string.setting_max_out), new Doer() {
            @Override
            public void execute() {
              editor.putLong( ListActivity.PREF_DB_MARKER, maxDB );
              editor.commit();
              // set the text on the other button
              tv.setText( getString(R.string.setting_max_id) + " " + maxDB );
            } 
          } );
        }
      } );
      
      // period spinners
      doScanSpinner( R.id.periodstill_spinner, 
          ListActivity.PREF_SCAN_PERIOD_STILL, ListActivity.SCAN_STILL_DEFAULT, getString(R.string.nonstop) );
      doScanSpinner( R.id.period_spinner, 
          ListActivity.PREF_SCAN_PERIOD, ListActivity.SCAN_DEFAULT, getString(R.string.nonstop) );
      doScanSpinner( R.id.periodfast_spinner, 
          ListActivity.PREF_SCAN_PERIOD_FAST, ListActivity.SCAN_FAST_DEFAULT, getString(R.string.nonstop) );
      doScanSpinner( R.id.gps_spinner, 
          ListActivity.GPS_SCAN_PERIOD, ListActivity.LOCATION_UPDATE_INTERVAL, getString(R.string.setting_tie_wifi) );
      
      MainActivity.prefBackedCheckBox(this, R.id.edit_showcurrent, ListActivity.PREF_SHOW_CURRENT, true);
      MainActivity.prefBackedCheckBox(this, R.id.use_metric, ListActivity.PREF_METRIC, false);
      MainActivity.prefBackedCheckBox(this, R.id.found_sound, ListActivity.PREF_FOUND_SOUND, true);
      MainActivity.prefBackedCheckBox(this, R.id.found_new_sound, ListActivity.PREF_FOUND_NEW_SOUND, true);
      MainActivity.prefBackedCheckBox(this, R.id.speech_gps, ListActivity.PREF_SPEECH_GPS, true);
      MainActivity.prefBackedCheckBox(this, R.id.circle_size_map, ListActivity.PREF_CIRCLE_SIZE_MAP, false);
      
      // speech spinner
      Spinner spinner = (Spinner) findViewById( R.id.speak_spinner );
      if ( ! TTS.hasTTS() ) {
        // no text to speech :(
        spinner.setEnabled( false );
        final TextView speakText = (TextView) findViewById( R.id.speak_text );
        speakText.setText(getString(R.string.no_tts));
      }
      
      

      final String[] languages = new String[]{ "", "en", "cs", "da", "de", "es", "fi", 
          "fr", "hi", "it", "iw", "ja", "ko", "nl", "no", "pl", "pt", "ru", "sv", "zh" };
      final String[] languageName = new String[]{ getString(R.string.auto), getString(R.string.language_en), 
          getString(R.string.language_cs), getString(R.string.language_da), getString(R.string.language_de), 
          getString(R.string.language_es), getString(R.string.language_fi), getString(R.string.language_fr), 
          getString(R.string.language_hi), getString(R.string.language_it), getString(R.string.language_iw), 
          getString(R.string.language_ja), getString(R.string.language_ko), getString(R.string.language_nl), 
          getString(R.string.language_no), getString(R.string.language_pl), getString(R.string.language_pt), 
          getString(R.string.language_ru), getString(R.string.language_sv), getString(R.string.language_zh),
          };
      doSpinner( R.id.language_spinner, ListActivity.PREF_LANGUAGE, "", languages, languageName );   
      
      final String off = getString(R.string.off);
      final String sec = " " + getString(R.string.sec);
      final String min = " " + getString(R.string.min);            
      
      final Long[] speechPeriods = new Long[]{ 10L,15L,30L,60L,120L,300L,600L,900L,1800L,0L };
      final String[] speechName = new String[]{ "10" + sec,"15" + sec,"30" + sec,
          "1" + min,"2" + min,"5" + min,"10" + min,"15" + min,"30" + min, off };
      doSpinner( R.id.speak_spinner, 
          ListActivity.PREF_SPEECH_PERIOD, ListActivity.DEFAULT_SPEECH_PERIOD, speechPeriods, speechName );      
            
      // battery kill spinner
      final Long[] batteryPeriods = new Long[]{ 1L,2L,3L,4L,5L,10L,15L,20L,0L };
      final String[] batteryName = new String[]{ "1 %","2 %","3 %","4 %","5 %","10 %","15 %","20 %",off };
      doSpinner( R.id.battery_kill_spinner, 
          ListActivity.PREF_BATTERY_KILL_PERCENT, ListActivity.DEFAULT_BATTERY_KILL_PERCENT, batteryPeriods, batteryName );   
      
      // reset wifi spinner
      final Long[] resetPeriods = new Long[]{ 15000L,30000L,60000L,90000L,120000L,300000L,600000L,0L };
      final String[] resetName = new String[]{ "15" + sec, "30" + sec,"1" + min,"1.5" + min,
          "2" + min,"5" + min,"10" + min,off };
      doSpinner( R.id.reset_wifi_spinner, 
          ListActivity.PREF_RESET_WIFI_PERIOD, ListActivity.DEFAULT_RESET_WIFI_PERIOD, resetPeriods, resetName );      
  }
  
  @Override
  public void onResume() {
    ListActivity.info( "resume settings." );
    
    final SharedPreferences prefs = this.getSharedPreferences( ListActivity.SHARED_PREFS, 0);
    // donate
    final boolean isDonate = prefs.getBoolean( ListActivity.PREF_DONATE, false);
    if ( isDonate ) {
      eraseDonate();
    }
    
    super.onResume();
  }
  
  private void updateRegister() {
    final TextView register = (TextView) findViewById(R.id.register);
    final SharedPreferences prefs = this.getSharedPreferences( ListActivity.SHARED_PREFS, 0);
    final String username = prefs.getString( ListActivity.PREF_USERNAME, "" );
    final boolean isAnonymous = prefs.getBoolean( ListActivity.PREF_BE_ANONYMOUS, false);
    if ( "".equals(username) || isAnonymous ) {
      register.setEnabled( true );
      register.setVisibility( View.VISIBLE );        
    }
    else {
      // poof
      register.setEnabled( false );
      register.setVisibility( View.GONE );
    }    
  }
  
  private void eraseDonate() {
    final CheckBox donate = (CheckBox) findViewById(R.id.donate);
    donate.setEnabled(false);
    donate.setVisibility(View.GONE);
  }
  
  private void doScanSpinner( final int id, final String pref, final long spinDefault, final String zeroName ) {
    final String ms = " " + getString(R.string.ms_short);
    final String sec = " " + getString(R.string.sec);
    final String min = " " + getString(R.string.min);
    
    final Long[] periods = new Long[]{ 0L,50L,250L,500L,750L,1000L,1500L,2000L,3000L,4000L,5000L,10000L,30000L,60000L };
    final String[] periodName = new String[]{ zeroName,"50" + ms,"250" + ms,"500" + ms,"750" + ms,
        "1" + sec,"1.5" + sec,"2" + sec,
        "3" + sec,"4" + sec,"5" + sec,"10" + sec,"30" + sec,"1" + min };
    doSpinner(id, pref, spinDefault, periods, periodName);
  }
  
  private <V> void doSpinner( final int id, final String pref, final V spinDefault, 
      final V[] periods, final String[] periodName ) {
    
    if ( periods.length != periodName.length ) {
      throw new IllegalArgumentException("lengths don't match, periods: " + Arrays.toString(periods)
          + " periodName: " + Arrays.toString(periodName));
    }
    
    final SharedPreferences prefs = this.getSharedPreferences( ListActivity.SHARED_PREFS, 0);
    final Editor editor = prefs.edit();
    
    Spinner spinner = (Spinner) findViewById( id );
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
        this, android.R.layout.simple_spinner_item);
    
    Object period = null;
    if ( periods instanceof Long[] ) {
      period = prefs.getLong( pref, (Long) spinDefault );
    }
    else if ( periods instanceof String[] ) {
      period = prefs.getString( pref, (String) spinDefault );
    }
    else {
      ListActivity.error("unhandled object type array: " + periods + " class: " + periods.getClass());
    }
    
    int periodIndex = 0;
    for ( int i = 0; i < periods.length; i++ ) {
      adapter.add( periodName[i] );
      if ( period.equals(periods[i]) ) {
        periodIndex = i;
      }
    }
    adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
    spinner.setAdapter( adapter );
    spinner.setSelection( periodIndex );
    spinner.setOnItemSelectedListener( new OnItemSelectedListener() {
      public void onItemSelected( final AdapterView<?> parent, final View v, final int position, final long id ) {
        // set pref
        final V period = periods[position];
        ListActivity.info( pref + " setting scan period: " + period );
        if ( period instanceof Long ) {
          editor.putLong( pref, (Long) period );
        }
        else if ( period instanceof String ) {
          editor.putString( pref, (String) period );
        }
        else {
          ListActivity.error("unhandled object type: " + period + " class: " + period.getClass());
        }
        editor.commit();
        
        if ( period instanceof String ) {          
          MainActivity.setLocale( SettingsActivity.this );           
        }

      }
      public void onNothingSelected( final AdapterView<?> arg0 ) {}
      });
  }    
  
  /* Creates the menu items */
  @Override
  public boolean onCreateOptionsMenu( final Menu menu ) {
      MenuItem item = menu.add(0, MENU_RETURN, 0, getString(R.string.menu_return));
      item.setIcon( android.R.drawable.ic_media_previous );
      
      item = menu.add( 0, MENU_ERROR_REPORT, 0, getString(R.string.menu_error_report) );
      item.setIcon( android.R.drawable.ic_menu_report_image );
      
      return true;
  }

  /* Handles item selections */
  @Override
  public boolean onOptionsItemSelected( final MenuItem item ) {
      switch ( item.getItemId() ) {
        case MENU_RETURN:
          finish();
          return true;
        case MENU_ERROR_REPORT:
          final Intent errorReportIntent = new Intent( this, ErrorReportActivity.class );
          this.startActivity( errorReportIntent );
          break;
      }
      return false;
  }    
  
}
