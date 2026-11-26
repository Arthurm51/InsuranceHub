package br.com.insurehub.policy.details;

import br.com.insurehub.common.Cpf;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Dados informados na contratação, por produto (RN-04). */
public final class ProductDetails {

    private ProductDetails() {}

    public enum PensionPlanType { PGBL, VGBL }

    public record Driver(
            @NotBlank(message = "Nome do condutor é obrigatório")
            @Size(min = 3, max = 120, message = "Nome do condutor deve ter entre 3 e 120 caracteres")
            String name,

            @NotBlank(message = "CPF do condutor é obrigatório")
            @Cpf(message = "CPF do condutor inválido")
            String cpf) {}

    public record Auto(
            @NotBlank(message = "Placa é obrigatória")
            @Pattern(regexp = "^[A-Za-z]{3}-?[0-9][A-Za-z0-9][0-9]{2}$",
                    message = "Placa deve estar no padrão AAA9999 ou Mercosul AAA9A99")
            String plate,

            @NotNull(message = "Informe ao menos 1 condutor")
            @Size(min = 1, max = 3, message = "Informe de 1 a 3 condutores")
            List<@Valid @NotNull Driver> drivers) {}

    public record Beneficiary(
            @NotBlank(message = "Nome do beneficiário é obrigatório")
            @Size(min = 3, max = 120, message = "Nome do beneficiário deve ter entre 3 e 120 caracteres")
            String name,

            @NotBlank(message = "Parentesco é obrigatório")
            @Size(min = 2, max = 40, message = "Parentesco deve ter entre 2 e 40 caracteres")
            String relationship,

            @NotNull(message = "Percentual é obrigatório")
            @Min(value = 1, message = "Percentual mínimo é 1")
            @Max(value = 100, message = "Percentual máximo é 100")
            Integer percentage) {}

    public record Vida(
            @NotNull(message = "Informe ao menos 1 beneficiário")
            @Size(min = 1, max = 5, message = "Informe de 1 a 5 beneficiários")
            List<@Valid @NotNull Beneficiary> beneficiaries) {}

    public record Previdencia(
            @NotNull(message = "Tipo do plano é obrigatório (PGBL ou VGBL)")
            PensionPlanType planType,

            @NotNull(message = "Informe ao menos 1 beneficiário")
            @Size(min = 1, max = 5, message = "Informe de 1 a 5 beneficiários")
            List<@Valid @NotNull Beneficiary> beneficiaries) {}

    public record Traveler(
            @NotBlank(message = "Nome do viajante é obrigatório")
            @Size(min = 3, max = 120, message = "Nome do viajante deve ter entre 3 e 120 caracteres")
            String name,

            @NotNull(message = "Data de nascimento do viajante é obrigatória")
            @Past(message = "Data de nascimento do viajante deve estar no passado")
            LocalDate birthDate) {}

    public record Viagem(
            @NotBlank(message = "Destino é obrigatório")
            @Size(min = 2, max = 80, message = "Destino deve ter entre 2 e 80 caracteres")
            String destination,

            @NotNull(message = "Informe ao menos 1 viajante")
            @Size(min = 1, max = 6, message = "Informe de 1 a 6 viajantes")
            List<@Valid @NotNull Traveler> travelers) {}

    public record Address(
            @NotBlank(message = "CEP é obrigatório")
            @Pattern(regexp = "^\\d{8}$", message = "CEP deve ter 8 dígitos")
            String zipCode,

            @NotBlank(message = "Logradouro é obrigatório")
            @Size(max = 120, message = "Logradouro deve ter no máximo 120 caracteres")
            String street,

            @NotBlank(message = "Número é obrigatório")
            @Size(max = 10, message = "Número deve ter no máximo 10 caracteres")
            String number,

            @Size(max = 60, message = "Complemento deve ter no máximo 60 caracteres")
            String complement,

            @NotBlank(message = "Bairro é obrigatório")
            @Size(max = 80, message = "Bairro deve ter no máximo 80 caracteres")
            String district,

            @NotBlank(message = "Cidade é obrigatória")
            @Size(max = 80, message = "Cidade deve ter no máximo 80 caracteres")
            String city,

            @NotBlank(message = "UF é obrigatória")
            @Pattern(regexp = "^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$",
                    message = "UF inválida")
            String state) {}

    public record Residencial(
            @NotNull(message = "Endereço é obrigatório")
            @Valid
            Address address) {}
}
