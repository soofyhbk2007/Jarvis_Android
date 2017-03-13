package com.iot.guc.jarvis.controllers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.iot.guc.jarvis.Constants;
import com.iot.guc.jarvis.HTTPResponse;
import com.iot.guc.jarvis.R;
import com.iot.guc.jarvis.Shared;
import com.iot.guc.jarvis.models.Device;
import com.iot.guc.jarvis.models.Room;
import com.iot.guc.jarvis.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private EditText LoginActivity_EditText_Username, LoginActivity_EditText_Password;
    private TextInputLayout LoginActivity_TextInputLayout_UsernameLayout, LoginActivity_TextInputLayout_PasswordLayout;
    private RelativeLayout LoginActivity_RelativeLayout_MainContentView, LoginActivity_RelativeLayout_LoginForm;
    private LinearLayout LoginActivity_LinearLayout_Progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        LoginActivity_EditText_Username = (EditText) findViewById(R.id.LoginActivity_EditText_Username);
        LoginActivity_EditText_Password = (EditText) findViewById(R.id.LoginActivity_EditText_Password);

        LoginActivity_TextInputLayout_UsernameLayout = (TextInputLayout) findViewById(R.id.LoginActivity_TextInputLayout_UsernameLayout);
        LoginActivity_TextInputLayout_PasswordLayout = (TextInputLayout) findViewById(R.id.LoginActivity_TextInputLayout_PasswordLayout);

        LoginActivity_RelativeLayout_MainContentView = (RelativeLayout) findViewById(R.id.LoginActivity_RelativeLayout_MainContentView);
        LoginActivity_RelativeLayout_LoginForm = (RelativeLayout) findViewById(R.id.LoginActivity_RelativeLayout_LoginForm);

        LoginActivity_LinearLayout_Progress = (LinearLayout) findViewById(R.id.LoginActivity_LinearLayout_Progress);


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String auth = sharedPreferences.getString("auth", "");
        if (!auth.isEmpty()) {
            showProgress(true);
            try {
                JSONObject user = new JSONObject(auth);
                Shared.setAuth(new User(user.getInt("id"), user.getString("username"), user.getString("token"), user.getString("type")));
                fetchRooms(false);
            } catch (JSONException e) {
                showProgress(false);
                Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Automatic Login Failed!", Snackbar.LENGTH_LONG).show();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("auth");
                editor.commit();
            }
        }
    }

    public void showProgress(boolean show) {
        LoginActivity_LinearLayout_Progress.setVisibility(show ? View.VISIBLE : View.GONE);
        LoginActivity_RelativeLayout_LoginForm.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    public void registerClicked(final View view) {
        LoginActivity_TextInputLayout_UsernameLayout.setError(null);
        LoginActivity_TextInputLayout_UsernameLayout.setErrorEnabled(false);
        LoginActivity_TextInputLayout_PasswordLayout.setError(null);
        LoginActivity_TextInputLayout_PasswordLayout.setErrorEnabled(false);

        if (LoginActivity_EditText_Username.getText().toString().isEmpty()) {
            LoginActivity_TextInputLayout_UsernameLayout.setErrorEnabled(true);
            LoginActivity_TextInputLayout_UsernameLayout.setError("Please Enter a Username");
        }

        if (LoginActivity_EditText_Password.getText().toString().isEmpty()) {
            LoginActivity_TextInputLayout_PasswordLayout.setErrorEnabled(true);
            LoginActivity_TextInputLayout_PasswordLayout.setError("Please Enter a Password");
        }

        if (LoginActivity_TextInputLayout_UsernameLayout.getError() == null && LoginActivity_TextInputLayout_PasswordLayout.getError() == null) {
            Shared.collapseKeyBoard(LoginActivity.this);
            showProgress(true);

            User.register(getApplicationContext(), LoginActivity_EditText_Username.getText().toString(), LoginActivity_EditText_Password.getText().toString(), new HTTPResponse() {
                @Override
                public void onSuccess(int statusCode, JSONObject body) {
                    loginClicked(findViewById(R.id.LoginActivity_Button_Login));
                }

                @Override
                public void onFailure(int statusCode, JSONObject body) {
                    showProgress(false);
                    switch (statusCode) {
                        case Constants.NO_INTERNET_CONNECTION: {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "No Internet Connection!", Snackbar.LENGTH_INDEFINITE).show();
                        }
                        break;
                        case Constants.SERVER_NOT_REACHED: {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Server Can\'t Be Reached!", Snackbar.LENGTH_INDEFINITE)
                            .setAction("RETRY", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    registerClicked(view);
                                }
                            }).show();
                        }
                        break;
                        case 400: {
                            try {
                                JSONArray arr = body.getJSONArray("errors");
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject current = arr.getJSONObject(i);
                                    String type = current.getString("msg");
                                    String field = current.getString("param");

                                    if (type.equals("required")) {
                                        if (field.equals("username")) {
                                            LoginActivity_TextInputLayout_UsernameLayout.setErrorEnabled(true);
                                            LoginActivity_TextInputLayout_UsernameLayout.setError("Please Enter a Username");
                                        }
                                        else if (field.equals("password")) {
                                            LoginActivity_TextInputLayout_PasswordLayout.setErrorEnabled(true);
                                            LoginActivity_TextInputLayout_PasswordLayout.setError("Please Enter a Password");
                                        }
                                        else {
                                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_INDEFINITE)
                                            .setAction("RETRY", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    registerClicked(view);
                                                }
                                            }).show();
                                            break;
                                        }
                                    }
                                    else if (type.equals("unique violation")) {
                                        if (field.equals("username")) {
                                            LoginActivity_TextInputLayout_UsernameLayout.setErrorEnabled(true);
                                            LoginActivity_TextInputLayout_UsernameLayout.setError("This username is already taken.");
                                        }
                                        else {
                                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_INDEFINITE)
                                            .setAction("RETRY", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    registerClicked(view);
                                                }
                                            }).show();
                                            break;
                                        }
                                    }
                                    else {
                                        Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_INDEFINITE)
                                        .setAction("RETRY", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                registerClicked(view);
                                            }
                                        }).show();
                                        break;
                                    }
                                }

                            } catch (JSONException e) {
                                Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_INDEFINITE)
                                .setAction("RETRY", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        registerClicked(view);
                                    }
                                }).show();
                                e.printStackTrace();
                            }

                        }
                        break;
                        default: {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_INDEFINITE)
                            .setAction("RETRY", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    registerClicked(view);
                                }
                            }).show();
                        }
                    }
                }
            });
        }
    }

    public void loginClicked(final View view) {
        LoginActivity_TextInputLayout_UsernameLayout.setError(null);
        LoginActivity_TextInputLayout_UsernameLayout.setErrorEnabled(false);
        LoginActivity_TextInputLayout_PasswordLayout.setError(null);
        LoginActivity_TextInputLayout_PasswordLayout.setErrorEnabled(false);

        if (LoginActivity_EditText_Username.getText().toString().isEmpty()) {
            LoginActivity_TextInputLayout_UsernameLayout.setErrorEnabled(true);
            LoginActivity_TextInputLayout_UsernameLayout.setError("Please Enter a Username");
        }

        if (LoginActivity_EditText_Password.getText().toString().isEmpty()) {
            LoginActivity_TextInputLayout_PasswordLayout.setErrorEnabled(true);
            LoginActivity_TextInputLayout_PasswordLayout.setError("Please Enter a Password");
        }

        if (LoginActivity_TextInputLayout_UsernameLayout.getError() == null && LoginActivity_TextInputLayout_PasswordLayout.getError() == null) {
            Shared.collapseKeyBoard(LoginActivity.this);
            showProgress(true);

            User.login(getApplicationContext(), LoginActivity_EditText_Username.getText().toString(), LoginActivity_EditText_Password.getText().toString(), new HTTPResponse() {
                @Override
                public void onSuccess(int statusCode, JSONObject body) {
                    try {
                        JSONObject user = body.getJSONObject("user");
                        Shared.setAuth(new User(user.getInt("id"), user.getString("username"), user.getString("token"), user.getString("type")));
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("auth", user.toString());
                        editor.commit();

                        fetchRooms(true);
                    } catch (JSONException e) {
                        showProgress(false);
                        Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                                .setAction("RETRY", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        loginClicked(view);
                                    }
                                }).show();
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, JSONObject body) {
                    showProgress(false);
                    switch (statusCode) {
                        case Constants.NO_INTERNET_CONNECTION: {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "No Internet Connection!", Snackbar.LENGTH_LONG).show();
                        }
                        break;
                        case Constants.SERVER_NOT_REACHED: {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Server Can\'t Be Reached!", Snackbar.LENGTH_LONG)
                                    .setAction("RETRY", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            loginClicked(view);
                                        }
                                    }).show();
                        }
                        break;
                        case 400: {
                            try {
                                JSONArray arr = body.getJSONArray("errors");
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject current = arr.getJSONObject(i);
                                    String type = current.getString("msg");
                                    String field = current.getString("param");

                                    if (type.equals("required")) {
                                        if (field.equals("username")) {
                                            LoginActivity_TextInputLayout_UsernameLayout.setErrorEnabled(true);
                                            LoginActivity_TextInputLayout_UsernameLayout.setError("Please Enter a Username");
                                        }
                                        else if (field.equals("password")) {
                                            LoginActivity_TextInputLayout_PasswordLayout.setErrorEnabled(true);
                                            LoginActivity_TextInputLayout_PasswordLayout.setError("Please Enter a Password");
                                        }
                                        else {
                                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                                                    .setAction("RETRY", new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            loginClicked(view);
                                                        }
                                                    }).show();
                                            break;
                                        }
                                    }
                                    else {
                                        Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                                                .setAction("RETRY", new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        registerClicked(view);
                                                    }
                                                }).show();
                                        break;
                                    }
                                }

                            } catch (JSONException e) {
                                Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                                        .setAction("RETRY", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                loginClicked(view);
                                            }
                                        }).show();
                                e.printStackTrace();
                            }

                        }
                        break;
                        case 401: {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Either The Username or Password is incorrect.", Snackbar.LENGTH_LONG).show();
                        }
                        break;
                        default: {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                                    .setAction("RETRY", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            loginClicked(view);
                                        }
                                    }).show();
                        }
                    }
                }
            });
        }
    }

    public void fetchRooms(final boolean fromForm) {
        Room.getRooms(getApplicationContext(), new HTTPResponse() {
            @Override
            public void onSuccess(int statusCode, JSONObject body) {
                try {
                    JSONArray rooms = body.getJSONArray("rooms");
                    for (int i = 0; i < rooms.length(); i++) {
                        JSONObject current = rooms.getJSONObject(i);
                        Shared.addRoom(new Room(current.getInt("id"), current.getString("name")));
                    }

                    fetchDevices(fromForm);
                } catch (JSONException e) {
                    showProgress(false);
                    if (fromForm) {
                    Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                fetchRooms(fromForm);
                            }
                        }).show();
                    }
                    else {
                        Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Automatic Login Failed!", Snackbar.LENGTH_LONG).show();
                    }

                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, JSONObject body) {
                showProgress(false);
                switch (statusCode) {
                    case Constants.NO_INTERNET_CONNECTION: {
                        Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "No Internet Connection!", Snackbar.LENGTH_LONG).show();
                    }
                    break;
                    case Constants.SERVER_NOT_REACHED: {
                        if (fromForm) {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                                    .setAction("RETRY", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            fetchRooms(fromForm);
                                        }
                                    }).show();
                        }
                        else {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Automatic Login Failed!", Snackbar.LENGTH_LONG).show();
                        }
                    }
                    break;
                    default: {
                        if (fromForm) {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                                    .setAction("RETRY", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            fetchRooms(fromForm);
                                        }
                                    }).show();
                        }
                        else {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Automatic Login Failed!", Snackbar.LENGTH_LONG).show();
                        }
                    }
                }
            }
        });
    }

    public void fetchDevices(final boolean fromForm) {
        Device.getDevices(getApplicationContext(), new HTTPResponse() {
            @Override
            public void onSuccess(int statusCode, JSONObject body) {
                try {
                    JSONArray devices = body.getJSONArray("devices");
                    for (int i = 0; i < devices.length(); i++) {
                        JSONObject current = devices.getJSONObject(i);
                        Shared.addDevice(new Device(current.getInt("id"), current.getString("name"),
                                current.getString("type").equals("Light Bulb") ? Device.TYPE.LIGHT_BULB : Device.TYPE.LOCK,
                                current.getBoolean("status"), current.getString("mac"), current.getString("ip"), current.getInt("room_id")));
                    }

                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                } catch (JSONException e) {
                    showProgress(false);
                    if (fromForm) {
                        Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                                .setAction("RETRY", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        fetchDevices(fromForm);
                                    }
                                }).show();
                    }
                    else {
                        Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Automatic Login Failed!", Snackbar.LENGTH_LONG).show();
                    }

                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, JSONObject body) {
                showProgress(false);
                switch (statusCode) {
                    case Constants.NO_INTERNET_CONNECTION: {
                        Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "No Internet Connection!", Snackbar.LENGTH_LONG).show();
                    }
                    break;
                    case Constants.SERVER_NOT_REACHED: {
                        if (fromForm) {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                                    .setAction("RETRY", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            fetchDevices(fromForm);
                                        }
                                    }).show();
                        }
                        else {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Automatic Login Failed!", Snackbar.LENGTH_LONG).show();
                        }
                    }
                    break;
                    default: {
                        if (fromForm) {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Something Went Wrong!", Snackbar.LENGTH_LONG)
                                    .setAction("RETRY", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            fetchDevices(fromForm);
                                        }
                                    }).show();
                        }
                        else {
                            Snackbar.make(LoginActivity_RelativeLayout_MainContentView, "Automatic Login Failed!", Snackbar.LENGTH_LONG).show();
                        }
                    }
                }
            }
        });
    }
}
