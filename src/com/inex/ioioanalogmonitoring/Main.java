package com.inex.ioioanalogmonitoring;

import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Gravity;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/* 
 * LCD Display
 *********************************************************************************************************
 *  LCD 16x2  |   RS   |    E   |   D0   |   D1   |   D2   |   D3   |   D4   |   D5   |   D6   |    D7   |
 *********************************************************************************************************
 * IOIO Board | Port 1 | Port 2 | Port 3 | Port 4 | Port 5 | Port 6 | Port 7 | Port 8 | Port 9 | Port 10 |
 *********************************************************************************************************
 * 
 * Analog Pin are Port 31 to 46
 * PWM output Port 34-40 and 45-48
 * 
 * Analog input & PWM output
 **********************************
 * Input Port |    VR   |    PZ   |
 **********************************
 * IOIO Board | Port 40 | Port 45 |
 **********************************
 * 
 */

public class Main extends IOIOActivity {

	// Create boolean to check warning state
	int stWarn = 0;
	
	// 0 : Idle state
	// 1 : Warning state
	// 2 : Waiting for idle state
	final int NORMAL_ST = 0;
	final int WARN_ST = 1;
	final int WAIT_ST = 2;
	
	// Create value to store warning voltage level (Between 0 - 3.3)
	float lvWarn;
	
	// Create object for widget
	private ProgressBar progressBar;
	private TextView txtVal;
	private Button btnSet;
	private EditText etxtVolt;
	
	// onCreate function that will be do first when application is startup
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set format of image that will be use in this application
        getWindow().setFormat(PixelFormat.RGBA_8888);
        
		// Application no notification bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Set application use layout from main.xml
		setContentView(R.layout.main);
		
				
		// Assign object to widget 
		txtVal = (TextView) findViewById(R.id.txtVal);
		btnSet = (Button) findViewById(R.id.btnSet);
		etxtVolt = (EditText) findViewById(R.id.etxtVolt);
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		
		// Set event listener to button
		btnSet.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				// Avoid error from blank input 
				// By checking the letter length in edit text
				if(etxtVolt.getText().length() == 0){
					// Set edit text to 0
					etxtVolt.setText("0");
				} 
				
				// Get warning voltage level form edit text
				lvWarn = Float.valueOf(etxtVolt.getText().toString());
				
				// If warning voltage level more than 3.3 V, set to 3.3 V
				if(lvWarn > 3.3){
					lvWarn = (float) 3.3;
					etxtVolt.setText("3.3");
				}
				
				// Toast the warning voltage level
				Toast.makeText(getApplicationContext(), "Set warning voltage to " 
						+ String.valueOf(lvWarn) + " V", Toast.LENGTH_SHORT).show();
			}
		});
		
		// Call value from storage in last time used
		SharedPreferences sp = getPreferences(MODE_PRIVATE);
		lvWarn = sp.getFloat("store", (float)2.7);
		
		// Set edit text to last value
		etxtVolt.setText(String.valueOf(lvWarn));
		
		// Custom Progress Bar Code
		// You can skip this code for Default Progressive Bar
		final float[] roundedCorners = new float[] { 5, 5, 5, 5, 5, 5, 5, 5 };
		ShapeDrawable pgDrawable = new ShapeDrawable(new RoundRectShape(roundedCorners, null,null));
		pgDrawable.getPaint().setColor(Color.parseColor("#DEDEDE"));
		ClipDrawable progress = new ClipDrawable(pgDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL);
		progressBar.setProgressDrawable(progress);  
	}
	
	// On resume function
	@Override
    public void onResume() {
        super.onResume();
	}
	
	// On pause function
	@Override
    public void onPause() {
        super.onPause();
    }
	
	// On stop function
	@Override
    public void onStop() {
        super.onStop();
        // Temporary lvWarn value to storage for use in next time
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat("store", lvWarn);
        editor.commit();
    }
	
	// On destroy function
	@Override
    public void onDestroy() {
        super.onDestroy();
    }

	// This class is thread for ioio board
	// You can control ioio board through this class 
	class Looper extends BaseIOIOLooper {
		// Create object for assigned to digital output port 
		DigitalOutput D0,D1,D2,D3,D4,D5,D6,D7,RS,E;

		// Create object for assigned to analog output port 
		AnalogInput ain;

		// Create object for assigned to PWM output port 
		PwmOutput pwm;
		
		// This function will do when application is startup 
		// Like onCreate function but use with ioio board
		@Override
		public void setup() throws ConnectionLostException {
			// Assigned eacth object to each digital output port and initial state is false
			D0 = ioio_.openDigitalOutput(3, false);
			D1 = ioio_.openDigitalOutput(4, false);
			D2 = ioio_.openDigitalOutput(5, false);
			D3 = ioio_.openDigitalOutput(6, false);
			D4 = ioio_.openDigitalOutput(7, false);
			D5 = ioio_.openDigitalOutput(8, false);
			D6 = ioio_.openDigitalOutput(9, false);
			D7 = ioio_.openDigitalOutput(10, false);
			RS = ioio_.openDigitalOutput(1, false);
			E = ioio_.openDigitalOutput(2, false);

			// Assigned object to analog output port
			ain = ioio_.openAnalogInput(40);
            
			// if we use any command which not ioio command 
			// in any ioio board's function program will force close
			// then we could use runnable to avoid force close
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// When device connected with ioio board 
					// Toast will show "Connected!"
					Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();
				}		
			});
			
			// Initial LCD Display (Twice for surely)
			lcd_init();
			lcd_init();
			
			// Show "Running" on LCD Display 
            Print("Running!", 0x80);
		}

		// This function will always running when device connect with ioio board
		// It use for control ioio board
		@Override
		public void loop() throws ConnectionLostException { 
			try {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							// Create string and read voltage from analog input port
							String str;
							str = String.valueOf(ain.getVoltage());
							
							// Set progress bar by reference from analog input port
							progressBar.setProgress((int)((float)(ain.getVoltage() * 100 / 3.3)));
							
							// Trim decimal to 1 digit 
							// If string longer than 3 letters
							// Then show result to text view
							if(str.length() > 3){
								txtVal.setText("Input voltage : " + str.substring(0,3) + " V");
							} else{
								txtVal.setText("Input voltage : " + str + " V");
							}
							
							// Check warning voltage 
							// If reading voltage more than warn value in idle state
							// set stWarn to 1 (warning state)
							if (ain.getVoltage() >= lvWarn && stWarn == NORMAL_ST) {
						        // Set stWarn to 1
								stWarn = WARN_ST;
							} 

							// If stWarn is 1
							// Genarate PWM and message
							// and set stWarn to 2 (Waiting for idle state)
							if (stWarn == WARN_ST) {
								// Show "Warning!" on LCD Display and toast
					            Print("Warning!", 0xC0);
								Toast.makeText(getApplicationContext(), "Warning!", Toast.LENGTH_SHORT).show();
								
								// Assigned object to PWM output port
								// on port 45 by 500 Hz
								pwm = ioio_.openPwmOutput(45, 500);
								
								// Set pulse width to 1000 microseconds
								pwm.setPulseWidth(1000);

						        // Set stWarn to 2
								stWarn = WAIT_ST;
							} 
							
							// If reading voltage less than warn value in waiting for idle state
							// Stop PWM, clear all and set back to idle state
							// And set stWarn to 0 (Idle State)
							if (ain.getVoltage() < lvWarn && stWarn == WAIT_ST) {
								// Close PWM port
								pwm.close();
								
								// Clear LCD Screen
								lcd_command(0x01);
								
								// Show "Running!" on LCD Display
						        Print("Running!", 0x80);
						        
						        // Set stWarn to 0
						        stWarn = NORMAL_ST;
						        
						        // Show "Normal" on toast
								Toast.makeText(getApplicationContext(), "Normal", Toast.LENGTH_SHORT).show();
							}							
						} catch (ConnectionLostException e) {
						} catch (InterruptedException e) { }
					}		
				});
				
				// Delay time 50 milliseconds
				Thread.sleep(50);
			} catch (InterruptedException e) {	}
		}
		
		// Function for send high pulse to LCD 
		public void enable() {
            try {
            	// Set e to be High
	            E.write(true);
	            
	            // Send high pulse for one millisecond
	            Thread.sleep(1);
	            
	            // Set back to Low 
				E.write(false);
			} catch (ConnectionLostException e) {
			} catch (InterruptedException e) { }
        }
		
		// Function for convert integer to boolean and send to data port on LCD
		public void senddatabit(int i) {
			// Call function for convert integer to boolean 
			// and set boolean logic to each port
			try {
				D0.write(check(i));
				D1.write(check(i >> 1));
				D2.write(check(i >> 2));
				D3.write(check(i >> 3));
				D4.write(check(i >> 4));
				D5.write(check(i >> 5));
				D6.write(check(i >> 6));
				D7.write(check(i >> 7));
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}        
			
			// Call enable function 
            enable();
        }

		// Function for convert interger value to boolean
		public boolean check(int i) {
			// Create variable for convert binary to boolean
			// Use for command LCD on IOIO Board
			boolean st = false;
			i = i & 0x01;
			
			// If i = 0 set st = false or if i =1 set st = true
			// and return st back to main program
			if(i == 0x00)
				st = false;
			else if(i == 0x01)
				st = true;
			return st;
		}
		
		// Send command to LCD
		public void lcd_command(int com) {
            try {
            	// Set rs port to low 
				RS.write(false);
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
            
            // Call senddatabit for send command
            senddatabit(com);
        }

		// Send command to LCD
		public void lcd_write(int text) {
            try {
            	// Set rs port to high 
				RS.write(true);
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
            
            // Call senddatabit for send data
            senddatabit(text);
        }		
		
		// Send data to LCD
		public void lcd_init() {
			// LCD 8 Bit 5x7 Dot 2 Line
			lcd_command(0x38);
			
			// Clear screen
			lcd_command(0x01);
			
			// Display on, no cursor
			lcd_command(0x0C);  
        }
		
		// Send one letters to LCD with set address 
		public void SendC(char c, int address) {
			// Set address
            lcd_command(address);
            
            // Send the letters to LCD
            lcd_write(Integer.valueOf(c));
        }
		
		// Send text string to LCD
		public void Print(String str, int address) {
			// Send the letters one by one until the end
            for (int i = 0; i < str.length(); i++) {
                SendC(str.charAt(i), address);
                address++;
            }
        }
	}
	
	@Override
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }
}