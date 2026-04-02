@Composable
fun CreateAvatarScreen(
    navController: NavController,
    viewModel: CreateAvatarViewModel = viewModel()
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text("Create Avatar", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            viewModel.saveDefaultAvatar(
                hair = "brown",
                skin = "light",
                imageUrl = "https://example.com/avatar.png"
            )
            navController.popBackStack()
        }) {
            Text("Standard Avatar wählen")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            navController.navigate("custom_avatar")
        }) {
            Text("Custom Avatar erstellen")
        }
    }
}