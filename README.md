# Objetivo

Utilizar a biblioteca GLPK para resolver problemas de Mix de Produção.

## Back End

Back End desenvolvido no IDE Eclipse com a linguagem Java e utilizado o serviço de hospedagem do Azure.

Url para POST: https://glpkotimiza.azurewebsites.net/

Exemplo de parâmetros JSON:

```json
[
   {
      "key":"Type",
      "value":"MAX",
      "description":""
   },
   {
      "key":"RestrictionCount",
      "value":"3",
      "description":""
   },
   {
      "key":"VariableCount",
      "value":"2",
      "description":""
   },
   {
      "key":"FO(Z)",
      "value":"14;x1 22;x2",
      "description":""
   },
   {
      "key":"R1",
      "value":"2;x1 4;x2 <= 250",
      "description":""
   },
   {
      "key":"R2",
      "value":"5;x1 8;x2 >= 460",
      "description":""
   },
   {
      "key":"R3",
      "value":"1;x1 0;x2 <= 40",
      "description":""
   },
   {
      "key":"Description",
      "value":"FO(Z) = 14x0 + 22x1;R1 -> 2x0 + 4x1 <= 250;R2 -> 5x0 + 8x1 >= 460;R3 -> 1x0 + 0x1 <= 40;",
      "description":""
   }
]
```

**Obs: se atentar a formatação dos parâmetros da requisição POST, olhar o código do Front End.**

## Front End

Desenvolvido no IDE Visual Studio com a linguagem C#.

## Imagens (Front End)

![FrontEnd 1](https://imgur.com/T6hsroc.png)

![FrontEnd 2](https://imgur.com/50xHw5K.png)

![FrontEnd 3](https://imgur.com/VsU3WxY.png)

## Créditos

**Pedro Pereira** (Front End, deploy e Back End)  
**Débora Deslandes** (ajuda no Back End)  
**Ícone** feito por [Smashicons](https://www.flaticon.com/authors/smashicons "Smashicons") do site www.flaticon.com# GLPK-Implementation
Implementação da biblioteca GLPK para java em nuvem (Webservice Java) e com front end feito em C#
