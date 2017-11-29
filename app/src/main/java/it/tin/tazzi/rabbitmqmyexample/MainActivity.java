package it.tin.tazzi.rabbitmqmyexample;

import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
{
    ConnectionFactory factory;
    Connection connection;
    Channel channel = null;
    Button button01;
    TextView textView;
    EditText editText;
    Thread subscribeThread;
    ConsumerTask001 Test001;
    ConsumerTask002 Test002;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button01 = (Button) findViewById(R.id.publish);
        button01.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v)
            {
                byte[] messageBodyBytes = editText.getText().toString().getBytes(Charset.forName("UTF-8"));
                try
                {
                    sendMessage(messageBodyBytes);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });

        textView = (TextView) findViewById(R.id.textView);
        editText = (EditText) findViewById(R.id.text);

        setupConnectionFactory();
    }

    private void sendMessage(byte[] messageBodyBytes) throws IOException
    {
        channel.basicPublish("MYEXCHANGE", "MYROUTINGKEY", true,
                MessageProperties.TEXT_PLAIN,
                messageBodyBytes);
    }


    @Override
    protected void onStop()
    {
        super.onStop();
        Test001.cancel(true);
    }

    private void setupConnectionFactory()
    {
        try
        {
            factory = new ConnectionFactory();
            factory.setAutomaticRecoveryEnabled(true);
            factory.setUri("amqp://USERNAME:PASSWORD@SERVERIP:5672/MYHOST");
            Test002 = new ConsumerTask002();

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                Test002.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                Test002.execute();
        }
        catch (Exception ex)
        {
            Toast.makeText(this, "Errore: " + ex, Toast.LENGTH_SHORT).show();
        }
    }

    class ConsumerTask001 extends AsyncTask<Void, Void, Boolean >
    {
        @Override
        protected Boolean  doInBackground(Void...params)
        {
            try
            {
                connection = factory.newConnection();
            }
            catch (Exception e)
            {
                Toast.makeText(getApplicationContext(), "Errore: " + e, Toast.LENGTH_SHORT).show();
            }

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    textView.append("Is Open: " + connection.isOpen());
                }
            });

            try
            {
                channel = connection.createChannel();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            try
            {
                channel.queueDeclare("MYQUEUE", true, false, false, null);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException
                {
                    final String message = new String(body, "UTF-8");
                    System.out.println(" [x] Received '" + message + "'");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                            textView.append("\n\n" + message);
                        }
                    });
                }
            };

            try
            {
                channel.basicConsume("MYQUEUE", true, consumer);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            while(true)
            {
                if(isCancelled()){
                    break;
                }
                System.out.println(" [*] Waiting for messages");
            }
            return true;
        }

        @Override
        protected void onPreExecute()
        {
        }

        @Override
        protected void onProgressUpdate(Void...values)
        {
        }

        @Override
        protected void onPostExecute(Boolean  result)
        {

        }

        @Override
        protected void onCancelled()
        {
            super.onCancelled();
            try
            {
                connection.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    //QUEUE RANDOM LISTEN ON AN EXCHANGE!!
    class ConsumerTask002 extends AsyncTask<Void, Void, Boolean >
    {
        @Override
        protected Boolean  doInBackground(Void...params)
        {
            try
            {
                connection = factory.newConnection();
            }
            catch (Exception e)
            {
                Toast.makeText(getApplicationContext(), "Errore: " + e, Toast.LENGTH_SHORT).show();
            }

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    textView.append("Is Open: " + connection.isOpen());
                }
            });

            try
            {
                channel = connection.createChannel();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            String queueName = "";
            try
            {
                channel.exchangeDeclare("MYEXCHANGE", "direct", true, true, null);
                queueName = channel.queueDeclare().getQueue();
                channel.queueBind(queueName, "MYEXCHANGE", "MYROUTINGKEY");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException
                {
                    final String message = new String(body, "UTF-8");
                    System.out.println(" [x] Received '" + message + "'");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.append("\n\n" + message);
                        }
                    });
                }
            };

            try
            {
                channel.basicConsume(queueName, true, consumer);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            while(true)
            {
                if(isCancelled()){
                    break;
                }
                System.out.println(" [*] Waiting for messages");
            }
            return true;
        }

        @Override
        protected void onPreExecute()
        {
        }

        @Override
        protected void onProgressUpdate(Void...values)
        {
        }

        @Override
        protected void onPostExecute(Boolean  result)
        {

        }

        @Override
        protected void onCancelled()
        {
            super.onCancelled();
            try
            {
                connection.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
