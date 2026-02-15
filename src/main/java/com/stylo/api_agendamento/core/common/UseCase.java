package com.stylo.api_agendamento.core.common;

import java.lang.annotation.*;

@Target(ElementType.TYPE) // Pode ser usada em Classes
@Retention(RetentionPolicy.RUNTIME) // Disponível em tempo de execução
@Documented
@Inherited
public @interface UseCase {
    // Essa anotação serve apenas como um "marcador"
}