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
        return List(productsArray.length()) { i ->
            parseSingleProduct(productsArray.getJSONObject(i))
        }
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
        val variantsArray = productJson.optJSONArray("variants")

        val imageUrl = imagesArray?.takeIf { it.length() > 0 }?.run {
            getJSONObject(0)
                .optJSONObject("attachment")
                ?.optString("original")
        }

        val productKey = productJson.optString("product_key")
        val hasVariants = variantsArray?.length()?.let { it > 0 } ?: false
        val variantId = if (!hasVariants) {
            masterJson?.optInt("id", -1)?.takeIf { it >= 0 }
        } else {
            null
        }

        return ProductModel(
            id = productJson.optInt("id", 0),
            productKey = productKey,
            sku = masterJson?.optString("sku"),
            description = productJson.optString("description"),
            variants = variantsArray,
            image = imageUrl,
            productSource = productJson.optString("source"),
            affiliateLink = masterJson?.optString("affiliate_link"),
            name = productJson.optString("name"),
            hasVariants = hasVariants,
            variantId = variantId
        )
    }
}