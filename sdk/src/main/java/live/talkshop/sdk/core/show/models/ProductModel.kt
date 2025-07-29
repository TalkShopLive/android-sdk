package live.talkshop.sdk.core.show.models

import com.google.gson.annotations.SerializedName
import org.json.JSONArray

/**
 * Represents the model for a product, encapsulating various details and metadata about it.
 *
 * @property id The unique identifier of the product.
 * @property sku The SKU (Stock Keeping Unit) identifier for the product.
 * @property description A description of the product.
 * @property variants A JSON array containing product variants.
 * @property image The URL for the product's image.
 * @property productSource The source or origin of the product.
 * @property affiliateLink The affiliate link for the product.
 * @property name The name for the product.
 */
data class ProductModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("sku")
    val sku: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("variants")
    val variants: JSONArray?,

    @SerializedName("image")
    val image: String?,

    @SerializedName("productSource")
    val productSource: String?,

    @SerializedName("affiliateLink")
    val affiliateLink: String?,

    @SerializedName("name")
    val name: String?,
)