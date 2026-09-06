package com.zyrdev.calculadorabreak;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Calculadora BREAK — Cafetería universitaria BREAK de La Juan.
 *
 * Actividad única de la aplicación. Su responsabilidad es:
 * 1. Capturar los datos de entrada (producto, precio unitario, cantidad).
 * 2. Validar que ningún campo esté vacío o inválido.
 * 3. Calcular el subtotal (precio unitario × cantidad).
 * 4. Aplicar el descuento según el rango del subtotal (0%, 5% o 10%).
 * 5. Mostrar el total a pagar con formato de moneda.
 * 6. Permitir limpiar la pantalla con el botón LIMPIAR.
 */
public class MainActivity extends AppCompatActivity {

    // CONSTANTES DE NEGOCIO (descuentos fijos definidos en el proyecto)

    /** Umbral inferior del rango con descuento del 5%. */
    private static final double DISCOUNT_RANGE_MIN = 50000.0;

    /** Umbral superior del rango con descuento del 5%. */
    private static final double DISCOUNT_RANGE_MAX = 100000.0;

    /** Porcentaje de descuento para subtotales menores a $50.000 (0%). */
    private static final double DISCOUNT_RATE_NONE = 0.0;

    /** Porcentaje de descuento para subtotales entre $50.000 y $100.000 (5%). */
    private static final double DISCOUNT_RATE_LOW = 0.05;

    /** Porcentaje de descuento para subtotales superiores a $100.000 (10%). */
    private static final double DISCOUNT_RATE_HIGH = 0.10;

    /** Formato de moneda para pesos colombianos (COP, sin decimales). */
    private static final NumberFormat CURRENCY_FORMAT =
            NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    // REFERENCIAS A LA INTERFAZ (declaradas como campos de clase)

    /** Campo de entrada: nombre del producto. */
    private EditText etProductName;

    /** Campo de entrada: precio unitario del producto. */
    private EditText etUnitPrice;

    /** Campo de entrada: cantidad de unidades. */
    private EditText etQuantity;

    /** Etiqueta de salida: subtotal calculado. */
    private TextView tvSubtotal;

    /** Etiqueta de salida: descuento aplicado. */
    private TextView tvDiscount;

    /** Etiqueta de salida: total a pagar (resaltado). */
    private TextView tvTotalAmount;

    // ============================================================
    // CICLO DE VIDA
    // ============================================================

    /**
     * Método de entrada de la Activity.
     * Infla el layout, vincula las vistas con sus IDs y registra
     * los listeners de los botones Calcular y Limpiar.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Vinculación de los campos de entrada con la interfaz
        etProductName = findViewById(R.id.et_product_name);
        etUnitPrice = findViewById(R.id.et_unit_price);
        etQuantity = findViewById(R.id.et_quantity);

        // Vinculación de las etiquetas de resultados
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDiscount = findViewById(R.id.tv_discount);
        tvTotalAmount = findViewById(R.id.tv_total_amount);

        // Registro del listener del botón CALCULAR
        Button btnCalculate = findViewById(R.id.btn_calculate);
        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculateTotal();
            }
        });

        // Registro del listener del botón LIMPIAR
        Button btnClear = findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearFields();
            }
        });
    }


    // FLUJO PRINCIPAL DE CÁLCULO


    /**
     * Orquesta el flujo completo de cálculo:
     * validación de entradas -> subtotal -> descuento -> total.
     * Si alguna validación falla, el flujo se detiene y la UI notifica
     * el error de forma poco intrusiva (setError en el campo erróneo).
     */
    private void calculateTotal() {
        // Verifica que ningún campo esté vacío o inválido antes de calcular
        String productName = etProductName.getText().toString().trim();
        String priceText = etUnitPrice.getText().toString().trim();
        String quantityText = etQuantity.getText().toString().trim();

        // Verifica que el nombre del producto no esté vacío
        if (productName.isEmpty()) {
            etProductName.setError(getString(R.string.error_empty_product));
            etProductName.requestFocus();
            return; // Detiene el flujo: hay un error de entrada
        }

        // Verifica que el precio no esté vacío
        if (priceText.isEmpty()) {
            etUnitPrice.setError(getString(R.string.error_empty_price));
            etUnitPrice.requestFocus();
            return;
        }

        // Verifica que la cantidad no esté vacía
        if (quantityText.isEmpty()) {
            etQuantity.setError(getString(R.string.error_empty_quantity));
            etQuantity.requestFocus();
            return;
        }

        // Convierte los textos numéricos a double; si el formato es
        // inválido (poco probable por el inputType), marca el campo
        double unitPrice;
        double quantity;
        try {
            unitPrice = Double.parseDouble(priceText);
            quantity = Double.parseDouble(quantityText);
        } catch (NumberFormatException e) {
            etUnitPrice.setError(getString(R.string.error_invalid_price));
            etUnitPrice.requestFocus();
            return;
        }

        // Verifica que el precio sea mayor a 0 (regla de negocio)
        if (unitPrice <= 0) {
            etUnitPrice.setError(getString(R.string.error_invalid_price));
            etUnitPrice.requestFocus();
            return;
        }

        // Verifica que la cantidad sea mayor a 0 (regla de negocio)
        if (quantity <= 0) {
            etQuantity.setError(getString(R.string.error_invalid_quantity));
            etQuantity.requestFocus();
            return;
        }

        // Entradas válidas: ejecuta el proceso matemático secuencial
        double subtotal = calculateSubtotal(unitPrice, quantity);
        double discountRate = calculateDiscount(subtotal);
        double discountAmount = subtotal * discountRate;
        double total = subtotal - discountAmount;

        // Salida: actualiza la vista con formato de moneda
        tvSubtotal.setText(CURRENCY_FORMAT.format(subtotal));
        tvDiscount.setText(formatPercent(discountRate));
        tvTotalAmount.setText(CURRENCY_FORMAT.format(total));
    }


    // MOTOR DE CÁLCULO (métodos privados de apoyo)

    /**
     * Calcula el subtotal de la venta.
     *
     * @param unitPrice precio unitario del producto.
     * @param quantity  cantidad de unidades.
     * @return subtotal resultante de multiplicar precio por cantidad.
     */
    private double calculateSubtotal(double unitPrice, double quantity) {
        // El subtotal es el precio unitario multiplicado por la cantidad
        return unitPrice * quantity;
    }

    /**
     * Determina el porcentaje de descuento según el rango del subtotal:
     *   - Menor a $50.000        -> 0%
     *   - Entre $50.000 y $100.000 -> 5%
     *   - Superior a $100.000    -> 10%
     *
     * @param subtotal subtotal calculado de la venta.
     * @return tasa de descuento (0.0, 0.05 o 0.10).
     */
    private double calculateDiscount(double subtotal) {
        // Subtotal menor a $50.000: sin descuento
        if (subtotal < DISCOUNT_RANGE_MIN) {
            return DISCOUNT_RATE_NONE;
        }
        // Subtotal entre $50.000 y $100.000 (inclusive): descuento del 5%
        if (subtotal <= DISCOUNT_RANGE_MAX) {
            return DISCOUNT_RATE_LOW;
        }
        // Subtotal superior a $100.000: descuento del 10%
        return DISCOUNT_RATE_HIGH;
    }


    // UTILIDADES DE FORMATO Y LIMPIEZA


    /**
     * Convierte una tasa de descuento (ej. 0.05) en texto legible (ej. "5%").
     *
     * @param discountRate tasa de descuento en decimales.
     * @return porcentaje formateado sin decimales.
     */
    private String formatPercent(double discountRate) {
        // Multiplica por 100 y elimina los decimales residuales
        return (int) Math.round(discountRate * 100) + "%";
    }

    /**
     * Restaura todos los campos y resultados a su estado inicial.
     * Se ejecuta al presionar el botón LIMPIAR.
     */
    private void clearFields() {
        // Vacía los campos de entrada
        etProductName.setText("");
        etUnitPrice.setText("");
        etQuantity.setText("");

        // Restaura los resultados a sus valores por defecto
        tvSubtotal.setText(getString(R.string.default_result_value));
        tvDiscount.setText(getString(R.string.default_discount_percent));
        tvTotalAmount.setText(getString(R.string.default_result_value));

        // Limpia cualquier mensaje de error previo en los campos
        etProductName.setError(null);
        etUnitPrice.setError(null);
        etQuantity.setError(null);

        // Devuelve el foco al primer campo para agilizar el siguiente pedido
        etProductName.requestFocus();
    }
}