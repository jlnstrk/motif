import SwiftUI
import Shared

struct ContentView: View {
    @StateObject var loginViewModel: LoginViewModelShim = LoginViewModelShim()

    var body: some View {
        // LoginView(viewModel: loginViewModel)
        switch loginViewModel.loginState {
        case is Shared.LoginState.LoggedIn:
            MainView()
        case is Shared.LoginState.LoggedOut:
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
