package e.commerce;

import e.commerce.AddressUI.Address;
import e.commerce.AddressUI.ShippingService;
import e.commerce.CouponResult; 


public interface ViewController {
    void showProductDetail(FavoritesUI.FavoriteItem product);
    void showFavoritesView();
    void showDashboardView();
    void showCartView();
    void showProfileView();
    void showOrdersView();
    void showCheckoutView();
    void showAddressView(CouponResult couponResult); 
    void showPaymentView(Address selectedAddress, ShippingService selectedShippingService, double totalAmount, CouponResult couponResult); // Metode baru
    void showSuccessView(int orderId);
    void showOrderDetailView(int orderId);
    void showChatWithSeller(int sellerId, String sellerUsername);
    void showProductReviewView(int productId, String productName, String productImage, double productPrice, String sellerName);
}
