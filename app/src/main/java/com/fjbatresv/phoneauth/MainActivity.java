package com.fjbatresv.phoneauth;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    @Bind(R.id.numero)
    EditText numero;
    @Bind(R.id.code)
    EditText code;
    @Bind(R.id.btn)
    Button btn;
    @Bind(R.id.result)
    TextView result;

    private String TAG = "PhoneAuthActivity";
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallback =  new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential credential) {
            // Este metodo es invocado en dos casos:
            // 1 - Verificación instantanea: En algunos casos el número puede ser verificado de forma
            //     automatica sin necesidad de codigo ni SMS.
            // 2 - Auto Revisión: En algunos dispositivos Google Play Services reconoce automaticamente
            //     el codigo y lo redime en la aplicación aún sin interacción del usuario.
            Log.e(TAG, "Verificacion completada:" + credential);
            signIn(credential);
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            // Esto se ejecuta cuando la llamada de verificación es invalida,
            // Por ejemplo el número no es valido.
            Log.e(TAG, "Verification fallida", e);

            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                // Petición invalida
                // ...
            } else if (e instanceof FirebaseTooManyRequestsException) {
                // Hemos excedido el limite de SMS del proyecto
                // ...
            }

            // Actualizar la UI con el error
            result.setText(e.getLocalizedMessage());
            result.setVisibility(View.VISIBLE);
        }

        @Override
        public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
            // El mensaje se ha enviado al numero para su verificacion,
            // ahora debemos pedirle al usuario que ingrese el codigo para verificar
            // Combinando el codigo con un verificationId.
            Log.e(TAG, "Codigo enviado:" + verificationId);
            result.setText(getString(R.string.message_sent));
            // Salvamos el varificationId y el token de reenvio para poder usarlos despues
            mVerificationId = verificationId;
            mResendToken = token;
            numero.setVisibility(View.GONE);
            code.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }


    @OnClick(R.id.btn)
    public void login(){
        if(numero.getVisibility() == View.VISIBLE){
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    numero.getText().toString(), //Numero al que envia el sms
                    60, //Tiempo de validez del codigo
                    TimeUnit.SECONDS, //Unidad del tiempo
                    this, //Actividad que recibe el accion siguiente
                    mCallback //Manejo del envio del sms
            );
        }else{
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code.getText().toString());
            signIn(credential);
        }
    }

    private void signIn(PhoneAuthCredential credential){
        Log.e(TAG, "Verificacion completada:" + credential.toString());
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            result.setText(getString(R.string.success) + " | Usuario: " + task.getResult().getUser().getUid() );
                        }else{
                            result.setText(getString(R.string.fail) + " | Exception: " + task.getException().getLocalizedMessage());
                        }
                        result.setVisibility(View.VISIBLE);
                    }
                });
    }

}
