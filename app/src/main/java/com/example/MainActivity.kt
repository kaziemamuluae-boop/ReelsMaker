package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.crop.CropScreen
import com.example.ui.editor.EditorScreen
import com.example.ui.export.ExportScreen
import com.example.ui.exported.ExportedVideosScreen
import com.example.ui.frame.FrameScreen
import com.example.ui.home.HomeScreen
import com.example.ui.layers.LayersScreen
import com.example.ui.logo.LogoScreen
import com.example.ui.projects.ProjectsScreen
import com.example.ui.selection.VideoSelectionScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.text.TextEditorScreen
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ReelsMakerTheme
import com.example.viewmodel.EditorViewModel

object ScreenRoutes {
    const val HOME = "home"
    const val SELECTION = "selection"
    const val EDITOR = "editor"
    const val CROP = "crop"
    const val TEXT_EDITOR = "text_editor"
    const val LOGO = "logo"
    const val FRAME = "frame"
    const val LAYERS = "layers"
    const val PROJECTS = "projects"
    const val EXPORT = "export"
    const val EXPORTED = "exported"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    private val editorViewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReelsMakerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepBlack
                ) {
                    ReelsMakerApp(viewModel = editorViewModel)
                }
            }
        }
    }
}

@Composable
fun ReelsMakerApp(viewModel: EditorViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ScreenRoutes.HOME
    ) {
        composable(ScreenRoutes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEditor = { navController.navigate(ScreenRoutes.EDITOR) },
                onNavigateToProjects = { navController.navigate(ScreenRoutes.PROJECTS) },
                onNavigateToExported = { navController.navigate(ScreenRoutes.EXPORTED) },
                onNavigateToSettings = { navController.navigate(ScreenRoutes.SETTINGS) },
                onNavigateToSelection = { navController.navigate(ScreenRoutes.SELECTION) }
            )
        }

        composable(ScreenRoutes.SELECTION) {
            VideoSelectionScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onVideoSelected = { navController.navigate(ScreenRoutes.EDITOR) }
            )
        }

        composable(ScreenRoutes.EDITOR) {
            EditorScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToCrop = { navController.navigate(ScreenRoutes.CROP) },
                onNavigateToText = { navController.navigate(ScreenRoutes.TEXT_EDITOR) },
                onNavigateToLogo = { navController.navigate(ScreenRoutes.LOGO) },
                onNavigateToFrame = { navController.navigate(ScreenRoutes.FRAME) },
                onNavigateToLayers = { navController.navigate(ScreenRoutes.LAYERS) },
                onNavigateToExport = { navController.navigate(ScreenRoutes.EXPORT) }
            )
        }

        composable(ScreenRoutes.CROP) {
            CropScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(ScreenRoutes.TEXT_EDITOR) {
            TextEditorScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(ScreenRoutes.LOGO) {
            LogoScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(ScreenRoutes.FRAME) {
            FrameScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(ScreenRoutes.LAYERS) {
            LayersScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToTextEdit = { navController.navigate(ScreenRoutes.TEXT_EDITOR) },
                onNavigateToLogoEdit = { navController.navigate(ScreenRoutes.LOGO) },
                onNavigateToFrameEdit = { navController.navigate(ScreenRoutes.FRAME) }
            )
        }

        composable(ScreenRoutes.PROJECTS) {
            ProjectsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onOpenProject = { navController.navigate(ScreenRoutes.EDITOR) },
                onNewProjectClick = { navController.navigate(ScreenRoutes.SELECTION) }
            )
        }

        composable(ScreenRoutes.EXPORT) {
            ExportScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToExported = {
                    navController.navigate(ScreenRoutes.EXPORTED) {
                        popUpTo(ScreenRoutes.HOME)
                    }
                }
            )
        }

        composable(ScreenRoutes.EXPORTED) {
            ExportedVideosScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(ScreenRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

