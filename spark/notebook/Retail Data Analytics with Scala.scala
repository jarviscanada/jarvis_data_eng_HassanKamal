// Databricks notebook source
// MAGIC %md
// MAGIC # Retail Data Wrangling and Analytics

// COMMAND ----------

//Import packages
import org.apache.spark.sql.functions.{col,max,min,mean,desc,expr,lag,countDistinct,lit, to_date, months_between}
import org.apache.spark.sql.expressions.Window

// COMMAND ----------

// MAGIC %md
// MAGIC ## Load CSV into Dataframe
// MAGIC 
// MAGIC - Read the `online_retail_II.csv` file into a DataFrame
// MAGIC - Rename all columns to upper camelcase or snakecase

// COMMAND ----------

val online_retail = spark.read.table("h_kamal_mail_utoronto_ca_db.online_retail")
.withColumnRenamed("Customer ID", "CustomerID")

online_retail.printSchema()

// COMMAND ----------

display(online_retail)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Total Invoice Amount Distribution

// COMMAND ----------

val total_invoice_df = online_retail
.groupBy(col("Invoice"))
.sum("Price")
.filter(col("Sum(Price)") > 0)
.withColumnRenamed("sum(Price)", "TotalAmount")

display(total_invoice_df)

// COMMAND ----------

val invoice_mode = total_invoice_df
.groupBy("TotalAmount")
.count()
.sort(desc("count"))
.select("TotalAmount")
.limit(5)
.collect()

val invoice_median = total_invoice_df.stat.approxQuantile("TotalAmount", Array(0.50), 0) (0)

val distribution_df = total_invoice_df
.select(max("TotalAmount").alias("Maximum"),min("TotalAmount").alias("Minimum"),mean("TotalAmount").alias("Mean"))
.withColumn("Median", lit(invoice_median))
.withColumn("Mode", lit(invoice_mode(0)(0)))

display(distribution_df)

// COMMAND ----------

//Another way
display(total_invoice_df.describe("TotalAmount"))

// COMMAND ----------

// MAGIC %md
// MAGIC ### Total Invoice Amount Distribution for first 85 Quantile

// COMMAND ----------

val quantile_85 = total_invoice_df.stat.approxQuantile("TotalAmount", Array(0.85), 0) (0)

val quantile_df = total_invoice_df.select("Invoice", "TotalAmount")
.filter(col("TotalAmount") < quantile_85.toInt)

val quantile_mode = quantile_df.groupBy("TotalAmount")
.count()
.sort(desc("count"))
.select("TotalAmount")
.limit(5)
.collect()

val quantile_median = quantile_df.stat.approxQuantile("TotalAmount", Array(0.50), 0) (0)

val quantile_distribution_df = quantile_df
.select(max("TotalAmount").alias("Maximum"),min("TotalAmount").alias("Minimum"),mean("TotalAmount").alias("Mean"))
.withColumn("Median", lit(quantile_median))
.withColumn("Mode", lit(quantile_mode(0)(0)))

display(quantile_distribution_df)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Monthly Placed and Canceled Orders

// COMMAND ----------

val improved_data_df = online_retail
.selectExpr("*", "date_format(InvoiceDate, 'YYYYMM') AS InvoiceYearMonth")
.sort("InvoiceYearMonth")

display(improved_data_df)

// COMMAND ----------

val orders_df = improved_data_df
.groupBy("Invoice", "InvoiceYearMonth")
.count()
.select("Invoice", "InvoiceYearMonth")

display(orders_df)

// COMMAND ----------

val cancelled_df = orders_df.filter(col("Invoice").rlike("^C"))
.groupBy("InvoiceYearMonth")
.count()
.withColumnRenamed("count","CancelledOrders")

display(cancelled_df)

// COMMAND ----------

val total_df = orders_df.groupBy("InvoiceYearMonth")
.count()
.withColumnRenamed("count","TotalOrders")

display(total_df)

// COMMAND ----------

val join_df = total_df
.join(cancelled_df, (total_df("InvoiceYearMonth") === cancelled_df("InvoiceYearMonth")), "inner")
.select(total_df("InvoiceYearMonth"), total_df("TotalOrders"), cancelled_df("CancelledOrders"))

val placed_df = join_df
.withColumn("PlacedOrders", (col("TotalOrders") - (col("CancelledOrders") * 2)))
.select("InvoiceYearMonth", "PlacedOrders")
.sort("InvoiceYearMonth")

display(placed_df)

// COMMAND ----------

val all_orders_df = placed_df
.join(cancelled_df, "InvoiceYearMonth")
.select(placed_df("InvoiceYearMonth"), placed_df("PlacedOrders"), cancelled_df("CancelledOrders"))
.sort(placed_df("InvoiceYearMonth"))

display(all_orders_df)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Monthly Sales

// COMMAND ----------

val sales_df = improved_data_df
.withColumn("Sales", (col("Quantity") * col("Price")))
.groupBy("InvoiceYearMonth")
.sum("Sales")
.withColumnRenamed("sum(Sales)", "Sales")
.sort("InvoiceYearMonth")

display(sales_df)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Monthly Sales Growth

// COMMAND ----------

val w = Window.orderBy("InvoiceYearMonth")

val calc_sales_growth_df = sales_df
.withColumn("LastMonth", lag("Sales", 1, 0).over(w))
.withColumn("SalesGrowth", ((col("Sales") - col("LastMonth"))/(col("LastMonth") * 100)))
.filter(col("LastMonth") =!= 0)

//display(calc_sales_growth_df)

val sales_growth_df = calc_sales_growth_df
.select("InvoiceYearMonth", "SalesGrowth")

display(sales_growth_df)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Monthly Active Users

// COMMAND ----------

val active_df = improved_data_df
.groupBy("InvoiceYearMonth")
.agg(countDistinct("CustomerID").alias("ActiveUsers"))

display(active_df)

// COMMAND ----------

// MAGIC %md
// MAGIC ## New and Existing Users

// COMMAND ----------

val first_purchase_df = improved_data_df
.sort("InvoiceYearMonth","CustomerID")
.filter(col("CustomerID").isNotNull)
.dropDuplicates("CustomerID")
.withColumnRenamed("InvoiceYearMonth", "FirstPurchaseYearMonth")
.withColumnRenamed("CustomerID", "FirstPurchaseCustomerID")
.select("FirstPurchaseCustomerID", "FirstPurchaseYearMonth")

display(first_purchase_df)

// COMMAND ----------

val new_users_df = first_purchase_df
.groupBy("FirstPurchaseYearMonth")
.count()
.sort("FirstPurchaseYearMonth")
.withColumnRenamed("count", "NewUsers")
.withColumnRenamed("FirstPurchaseYearMonth", "InvoiceYearMonth")

display(new_users_df)

// COMMAND ----------

val joined_df = improved_data_df
.join(first_purchase_df, (improved_data_df("CustomerID") === first_purchase_df("FirstPurchaseCustomerID")), "inner")

val existing_users_df = joined_df
.select("CustomerID", "InvoiceYearMonth", "FirstPurchaseYearMonth")
.filter(col("InvoiceYearMonth") =!= col("FirstPurchaseYearMonth"))
.dropDuplicates("CustomerID", "InvoiceYearMonth")
.groupBy("InvoiceYearMonth")
.count()
.sort("InvoiceYearMonth")
.withColumnRenamed("count", "ExistingUsers")

display(existing_users_df)

// COMMAND ----------

val all_users_df = new_users_df
.join(existing_users_df, (new_users_df("InvoiceYearMonth") === existing_users_df("InvoiceYearMonth")), "left")
.select(new_users_df("InvoiceYearMonth"), new_users_df("NewUsers"), existing_users_df("ExistingUsers"))
.sort("InvoiceYearMonth")
.na.fill(0, Seq("ExistingUsers"))

display(all_users_df)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Finding RFM

// COMMAND ----------

val recency_df = improved_data_df
.withColumn("CurrentDate", to_date(lit("2012-01")))
.withColumn("Recency(Months)", months_between(col("CurrentDate"),col("InvoiceDate")))
.sort("Recency(Months)")
.dropDuplicates("CustomerID")
.select("CustomerID", "Recency(Months)")

display(recency_df)

// COMMAND ----------

val frequency_df = improved_data_df
.dropDuplicates("CustomerID", "Invoice")
.groupBy("CustomerID")
.count()
.withColumnRenamed("count", "Frequency")

display(frequency_df)

// COMMAND ----------

val monetary_df = improved_data_df
.withColumn("Sales", (col("Quantity") * col("Price")))
.groupBy("CustomerID")
.sum("Sales")
.withColumnRenamed("sum(Sales)", "Monetary")

display(monetary_df)

// COMMAND ----------

val rfm_df = recency_df
.join(frequency_df, "CustomerID")
.join(monetary_df, "CustomerID")
.sort("CustomerID")

display(rfm_df)
