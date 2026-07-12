package com.composea11yscanner.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrokenFormScreen(onViewFixed: (() -> Unit)? = null) {
    BrokenScreenCard(
        title = "Payment form",
        subtitle = "Transfer details with intentionally broken focus order",
    ) {
        var amount by remember { mutableStateOf("") }
        var recipient by remember { mutableStateOf("") }
        var reference by remember { mutableStateOf("") }
        var paymentDate by remember { mutableStateOf("") }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
                .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                .padding(16.dp),
        ) {
            PaymentSubmitAction(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
            )

            PaymentAmountField(
                amount = amount,
                onAmountChange = { amount = it },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
            )

            RecipientRow(
                recipient = recipient,
                onRecipientChange = { recipient = it },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 116.dp)
                    .fillMaxWidth(),
                isFixed = false,
            )

            OutlinedTextField(
                value = paymentDate,
                onValueChange = { paymentDate = it },
                label = { Text("Payment Date") },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 210.dp)
                    .fillMaxWidth(),
            )

            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Reference Note") },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 294.dp)
                    .fillMaxWidth(),
            )
        }

        onViewFixed?.let {
            Button(onClick = it, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("View Fixed Version")
            }
        }
    }
}

@Composable
fun FixedFormScreen(onViewBroken: (() -> Unit)? = null) {
    BrokenScreenCard(
        title = "Payment form",
        subtitle = "Accessible transfer details with top-to-bottom focus order",
    ) {
        var amount by remember { mutableStateOf("") }
        var recipient by remember { mutableStateOf("") }
        var reference by remember { mutableStateOf("") }
        var paymentDate by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PaymentAmountField(
                amount = amount,
                onAmountChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
            )

            RecipientRow(
                recipient = recipient,
                onRecipientChange = { recipient = it },
                modifier = Modifier.fillMaxWidth(),
                isFixed = true,
            )

            OutlinedTextField(
                value = paymentDate,
                onValueChange = { paymentDate = it },
                label = { Text("Payment Date") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Reference Note") },
                modifier = Modifier.fillMaxWidth(),
            )

            PaymentSubmitAction(modifier = Modifier.fillMaxWidth())
        }

        onViewBroken?.let {
            Button(
                onClick = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("View Broken Version")
            }
        }
    }
}

@Composable
private fun PaymentAmountField(
    amount: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = amount,
        onValueChange = onAmountChange,
        label = { Text("Amount") },
        prefix = {
            Text(
                text = "$",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        textStyle = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = modifier,
    )
}

@Composable
private fun RecipientRow(
    recipient: String,
    onRecipientChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isFixed: Boolean,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF6C63FF), RoundedCornerShape(28.dp))
                .then(
                    if (isFixed) {
                        Modifier.semantics { contentDescription = "Recipient avatar for Maya Johnson" }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "MJ",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        OutlinedTextField(
            value = recipient,
            onValueChange = onRecipientChange,
            label = { Text("Recipient Account") },
            modifier = Modifier.weight(1f),
        )
    }
}
