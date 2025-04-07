package live.talkshop.sdk.utils.parsers

import live.talkshop.sdk.core.show.models.ProductModel
import org.json.JSONObject

internal object ProductParser {
    /**
     * Parses a JSON object to create a list of ProductModel instances.
     *
     * @param json The JSON object containing product data.
     * @return A list of ProductModel objects populated with data from the JSON object.
     */
    fun parseFromJson(json: JSONObject): List<ProductModel> {
        val productsArray = json.getJSONArray("products")
        val productList = mutableListOf<ProductModel>()

        for (i in 0 until productsArray.length()) {
            val productJson = productsArray.getJSONObject(i)
            productList.add(parseSingleProduct(productJson))
        }

        return productList
    }

    /**
     * Parses a single product JSON object into a ProductModel instance.
     *
     * @param productJson The JSON object containing the product's details.
     * @return A ProductModel instance populated with data from the JSON object.
     */
    private fun parseSingleProduct(productJson: JSONObject): ProductModel {
        val masterJson = productJson.optJSONObject("master")
        val imagesArray = productJson.optJSONArray("images")

        val imageUrl = imagesArray?.let {
            if (it.length() > 0) {
                val firstImage = it.getJSONObject(0)
                firstImage.optJSONObject("attachment")?.optString("original")
            } else {
                null
            }
        }

        return ProductModel(
            id = productJson.optInt("id", 0),
            sku = masterJson?.optString("sku"),
            description = productJson.optString("description"),
            variants = productJson.optJSONArray("variants"),
            image = imageUrl,
            productSource = productJson.optString("source"),
            affiliateLink = masterJson?.optString("affiliate_link"),
            name = productJson.optString("name")
        )
    }
}