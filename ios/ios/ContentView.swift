import SwiftUI
import Shared

struct ContentView: View {
    @StateObject var loginViewModel: LoginViewModelShim = LoginViewModelShim()

    var body: some View {
        switch loginViewModel.loginState {
        case is Shared.LoginStateLoggedIn:
            MainView()
        case is Shared.LoginStateLoggedOut:
            LoginView(viewModel: loginViewModel)
        default:
            EmptyView()
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
