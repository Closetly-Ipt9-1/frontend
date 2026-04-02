@Composable
fun CustomAvatarScreen(
    navController: NavController,
    viewModel: CreateAvatarViewModel = viewModel()
) {

    var frontUri by remember { mutableStateOf<Uri?>(null) }
    var sideUri by remember { mutableStateOf<Uri?>(null) }

    val frontLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> frontUri = uri }

    val sideLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> sideUri = uri }

    Column(modifier = Modifier.padding(24.dp)) {

        Button(onClick = {
            frontLauncher.launch("image/*")
        }) {
            Text("Front Bild hochladen")
        }

        Button(onClick = {
            sideLauncher.launch("image/*")
        }) {
            Text("Seiten Bild hochladen")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            enabled = frontUri != null && sideUri != null,
            onClick = {
                viewModel.saveCustomAvatar(
                    front = frontUri.toString(),
                    side = sideUri.toString()
                )
                navController.popBackStack()
            }
        ) {
            Text("Speichern")
        }
    }
}