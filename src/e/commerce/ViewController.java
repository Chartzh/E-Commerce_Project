package e.commerce;

// Interface ini mendefinisikan metode untuk beralih antara panel UI yang berbeda.
// Ini memisahkan panel UI (seperti FavoritesUI) dari implementasi JFrame yang konkret.
public interface ViewController {
    void showProductDetail(FavoritesUI.FavoriteItem product);
    void showFavoritesView();
    void showDashboardView();
    void showCartView();
    void showProfileView();
    void showOrdersView();
    void showCheckoutView();
    void showAddressView();
    void showPaymentView();
}