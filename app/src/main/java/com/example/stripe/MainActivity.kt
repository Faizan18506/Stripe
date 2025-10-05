package com.example.stripe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.stripe.ui.theme.StripeTheme
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val publishableKey = "pk_test_51SEZihHDhrYSyk215Aea5TW1iwjaff9EnJLDsdCy6XXIwMCa2ZjhNkjMp3LO8VYnBljS7uJnt4BxTw4rSgfZnzQx00ypk6BXEC"
    private val secretKey = "sk_test_51SEZihHDhrYSyk21BQb0OO2Oct7zHpPUm7NvKBZFrHGhz6NUPcbTRcwrWpxcYfWfv9jAtnFFx9t8C3z8bjCDFVrt00xeFW32tp"
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        PaymentConfiguration.init(applicationContext, publishableKey)
        enableEdgeToEdge()
        setContent {
            StripeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StripePaymentScreen(secretKey)
                }
            }
        }
    }
}

@Composable
fun StripePaymentScreen(secretKey: String) {
    var paymentStatus by remember { mutableStateOf("Ready to pay") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val stripeService = remember { StripeService(secretKey) }

    // Payment Sheet
    val paymentSheet = rememberPaymentSheet { result ->
        when (result) {
            is PaymentSheetResult.Completed -> {
                paymentStatus = "✓ Payment Successful!"
                isLoading = false
            }
            is PaymentSheetResult.Canceled -> {
                paymentStatus = "Payment Canceled"
                isLoading = false
            }
            is PaymentSheetResult.Failed -> {
                paymentStatus = "Failed: ${result.error.message}"
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Stripe Payment Test",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Amount to Pay",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$20.00",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = paymentStatus,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp),
            color = if (paymentStatus.contains("Successful"))
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        }

        Button(
            onClick = {
                isLoading = true
                paymentStatus = "Creating payment..."

                scope.launch {
                    try {
                        val result = stripeService.createPaymentIntent(2000) // $20.00
                        if (result.isSuccess) {
                            val clientSecret = result.getOrNull()
                            if (clientSecret != null) {
                                paymentStatus = "Opening payment sheet..."
                                presentPaymentSheet(paymentSheet, clientSecret)
                            } else {
                                paymentStatus = "Error: No client secret"
                                isLoading = false
                            }
                        } else {
                            paymentStatus = "Error: ${result.exceptionOrNull()?.message}"
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        paymentStatus = "Error: ${e.message}"
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (isLoading) "Processing..." else "Pay Now",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Test Cards",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "• Success: 4242 4242 4242 4242",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Decline: 4000 0000 0000 0002",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Expiry: Any future date (12/34)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• CVC: Any 3 digits (123)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun rememberPaymentSheet(
    onResult: (PaymentSheetResult) -> Unit
): PaymentSheet {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as ComponentActivity

    return remember {
        PaymentSheet(activity, onResult)
    }
}

fun presentPaymentSheet(paymentSheet: PaymentSheet, clientSecret: String) {
    val configuration = PaymentSheet.Configuration(
        merchantDisplayName = "Test Store",
        allowsDelayedPaymentMethods = false
    )

    paymentSheet.presentWithPaymentIntent(
        paymentIntentClientSecret = clientSecret,
        configuration = configuration
    )
}