package sv.edu.udb.eventoscomunitarios.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import sv.edu.udb.eventoscomunitarios.R
import sv.edu.udb.eventoscomunitarios.data.FirebaseAuthRepository
import sv.edu.udb.eventoscomunitarios.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private var binding: FragmentLoginBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return requireNotNull(binding).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.continueButton?.setOnClickListener {
            if (validateLogin()) {
                login()
            }
        }
    }

    private fun login() {
        val currentBinding = binding ?: return
        val email = currentBinding.emailInput.text?.toString()?.trim().orEmpty()
        val password = currentBinding.passwordInput.text?.toString().orEmpty()

        currentBinding.continueButton.isEnabled = false
        FirebaseAuthRepository.loginOrCreateUser(email, password) { result ->
            val activeBinding = binding ?: return@loginOrCreateUser
            activeBinding.continueButton.isEnabled = true

            result
                .onSuccess {
                    findNavController().navigate(R.id.action_loginFragment_to_eventListFragment)
                }
                .onFailure {
                    Toast.makeText(requireContext(), R.string.error_login_failed, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun validateLogin(): Boolean {
        val currentBinding = binding ?: return false
        val email = currentBinding.emailInput.text?.toString()?.trim().orEmpty()
        val password = currentBinding.passwordInput.text?.toString().orEmpty()

        currentBinding.emailInputLayout.error = when {
            email.isBlank() -> getString(R.string.error_required)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> getString(R.string.error_email_invalid)
            else -> null
        }

        currentBinding.passwordInputLayout.error = when {
            password.isBlank() -> getString(R.string.error_required)
            password.length < 6 -> getString(R.string.error_password_length)
            else -> null
        }

        return currentBinding.emailInputLayout.error == null &&
            currentBinding.passwordInputLayout.error == null
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
