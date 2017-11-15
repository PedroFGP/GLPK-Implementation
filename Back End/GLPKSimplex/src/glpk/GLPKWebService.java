package glpk;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

class GLPKSimplex {

	//Variáveis locais para utilização do GLPK
	private static glp_prob Problem;
	private static glp_smcp Params;
	private static SWIGTYPE_p_int Indexes;
	private static SWIGTYPE_p_double Values;
	
	//Método para criação do problema no GLPK 
	public static void InitializeProblem(String problemName, int constraintsCount)
	{
		//Cria um novo problema
		Problem = GLPK.glp_create_prob();
		
		//Atribui um nome ao problema
		GLPK.glp_set_prob_name(Problem, problemName);
		
		//Adiciona o número de linhas(restrições) ao problema
		GLPK.glp_add_rows(Problem, constraintsCount);
	}

	//Método para atribuição da função objetiva para o problema criado no GLPK
	public static void SetObjectiveFunction(String ofRepresentation, int variableCount, boolean isMax)
	{
		//Adiciona o número de colunas(variáveis) ao problema
		GLPK.glp_add_cols(Problem, variableCount);
		
		//Atribui o nome da função objetiva no problema
		GLPK.glp_set_obj_name(Problem, "FO(Z)");
		
		//Atribui o tipo da função objetiva (Maximização ou Minização) no problema
		GLPK.glp_set_obj_dir(Problem, (isMax) ? GLPKConstants.GLP_MAX : GLPKConstants.GLP_MIN);

		//Divide a string da função objetiva por espaços(" ")
		String[] zSplit = ofRepresentation.split(" ");

		int varCounter = 1;

		for(String subZ : zSplit)
		{
			//Divide cada divisão da FO em ponto e vírgula(";")
			String[] member = subZ.split(";");

			//Extrai o valor da string
			double value = Double.parseDouble(member[0]);

			//Atribui o nome da variável a coluna do problema
			GLPK.glp_set_col_name(Problem, varCounter, member[1]);
			
			//Define que todas as variáveis têm de ser maiores ou iguais a 0
			GLPK.glp_set_col_bnds(Problem, varCounter, GLPKConstants.GLP_LO, 0, 0);

			//Determina se a variável é double ou int pela presença ou não do ponto
			boolean isInt = !member[0].contains(".");

			//Atribui o tipo da variável (Double ou int)
			GLPK.glp_set_col_kind(Problem, varCounter, (isInt) ? GLPKConstants.GLP_IV : GLPKConstants.GLP_CV);
			
			//Atribui o coeficiente da variável na FO
			GLPK.glp_set_obj_coef(Problem, varCounter, value);
			
			++varCounter;
		}
	}

	//Método para atribuição das restrições para o problema criado no GLPK
	public static void AddConstraints(Map<String, String> constraints)
	{
		//Pega quantas variáveis existem no problema
		int size = GLPK.glp_get_num_cols(Problem);

		//Aloca memória pois o GLPK é uma library nativa (C/C++)
		Indexes = GLPK.new_intArray(size);
		Values = GLPK.new_doubleArray(size);

		int constraintCount = 1;
		int varCount = 1;
		
		//Para cada restrição (string)
		for(Map.Entry<String, String> entry : constraints.entrySet())
		{
			//Atribui o nome da linha no problema ao "nome" da restrição
			GLPK.glp_set_row_name(Problem, constraintCount, entry.getKey());
			
			for(int i = 1; i <= size; i++)
			{
				GLPK.intArray_setitem(Indexes, i, i);
			}

			//Pega o valor em string da restrição
			String entryValue = entry.getValue();

			//Divide a restrição por espaços (" ")
			String[] members = entryValue.split(" ");

			varCount = 1;
			
			//Para cada parte separada por espaço (" ")
			for(String member : members)
			{
				//Se contem ";" indicando que é um coeficiente com variável
				if(member.contains(";"))
				{
					//Divide a parte por ponto e vírgula (";")
					String[] parts = member.split(";");

					//Pega o valor do coeficiente
					double partValue = Double.parseDouble(parts[0]);

					//Atribui o coeficiente ao item (variavel) correto
					GLPK.doubleArray_setitem(Values, varCount, partValue);
					
					++varCount;
				}
				//Se for ">=" ou "<=" "continuar" a iteração
				else if(member.contains(">=") || member.contains("<="))
				{
					continue;
				}
				else
				{
					//Define um limite inferior e superior como zero
					double loBound = 0, upBound = 0;

					//Se for maior ou igual define o limite inferior, caso contrário o inferior
					if(entryValue.contains(">="))
					{
						loBound = Double.parseDouble(member);
					}
					else
					{
						upBound = Double.parseDouble(member);
					}

					//Atribui os limites a aquela restrição dentro do problema
					GLPK.glp_set_row_bnds(Problem, constraintCount, (loBound != 0) ? GLPKConstants.GLP_LO : GLPKConstants.GLP_UP, loBound, upBound);
				}
			}

			//Atribui a restrição para o problema em seu lugar correto na matriz
			GLPK.glp_set_mat_row(Problem, constraintCount, size, Indexes, Values);
			
			++constraintCount;
		}

		//Libera a memória alocada
		GLPK.delete_intArray(Indexes);
		GLPK.delete_doubleArray(Values);
	}

	//Método apra resolver o problema
	public static String Solve()
	{
		try
		{
			//Instancia parâmetros utilizados pelo resolvedor de Simplex do GLPK
			Params = new glp_smcp();
			GLPK.glp_init_smcp(Params);
			String returnMessage = "";
	
			//Verifica se o problema foi ou não resolvido
			if (GLPK.glp_simplex(Problem, Params) == 0) 
			{
				//Caso tenho sido retornar a solução "completa"
				returnMessage = GetPrintableSolution();
			} 
			else 
			{
				returnMessage =  "O problema nao pode ser resolvido.";
			}
			
			//Deletar o problema do GLPK, liberando memória...
			GLPK.glp_delete_prob(Problem);
			
			return returnMessage;
		}
		catch(RuntimeException ex)
		{
			//Caso haja erro retornar o stack trace...
			return ex.getStackTrace().toString();
		}
	}
	
	public static String GetPrintableSolution()
	{
		int i;
		int n;
		String name, message = "";
		double val;

		//Pegar o código do status da solução do problema
		int problemStatus = GLPK.glp_get_status(Problem);
		
		//Traduzir o código do status
		if(problemStatus == GLPKConstants.GLP_OPT)
		{
			message += "Solucao otima encontrada!";
		}
		else if(problemStatus == GLPKConstants.GLP_UNDEF)
		{
			message += "Solucao indefinida!";
		}
		else if(problemStatus == GLPKConstants.GLP_INFEAS || problemStatus == GLPKConstants.GLP_NOFEAS)
		{
			message += "Solucao e impraticavel!";
		}
		
		message += "\n\n";
		
		//Recuperar o valor para função objetiva
		name = GLPK.glp_get_obj_name(Problem);
		val = GLPK.glp_get_obj_val(Problem);
		
		message += name + " = " + val + "\n";
		
		n = GLPK.glp_get_num_cols(Problem);
		
		//Recuperar o valor para as variáveis do problema
		for (i = 1; i <= n; i++) 
		{
			name = GLPK.glp_get_col_name(Problem, i);
			val = GLPK.glp_get_col_prim(Problem, i);

			message += name + " = " + val + "\n";
		}

		//Retornar a mensagem montada
		return message;
	}
}

/**
 * Servlet implementation class WebService
 */
@WebServlet("/")
public class GLPKWebService extends HttpServlet 
{
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GLPKWebService() 
	{
		super();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */    
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		//Recuperar os valores dos parâmetros da requisição POST
		String type = request.getParameter("Type");
		String of = request.getParameter("FO(Z)");
		String problemDescription = request.getParameter("Description");
		int varCount = Integer.parseInt(request.getParameter("VariableCount"));
		int restrictionCount = Integer.parseInt(request.getParameter("RestrictionCount"));

		Map<String, String[]> parameterMap = new HashMap<String, String[]>(request.getParameterMap());

		//Remover os parâmetros já recuperados
		parameterMap.remove("Type");
		parameterMap.remove("VariableCount");
		parameterMap.remove("RestrictionCount");
		parameterMap.remove("FO(Z)");
		parameterMap.remove("Description");

		Map<String,String> constraints = new HashMap<String, String>();

		//Para cada parâmetro restante adicioná lo em um mapeamento composto do seu nome e seu valor
		for(String key : parameterMap.keySet())
		{
			constraints.put(key, request.getParameter(key));
		}

		//Inicializar o problema no GLPK
		GLPKSimplex.InitializeProblem("Problema de Mix de Producao", restrictionCount);
		
		//Definir a FO no GLPK
		GLPKSimplex.SetObjectiveFunction(of, varCount, type.equals("MAX"));
		
		//Definir as restrições no GLPK
		GLPKSimplex.AddConstraints(constraints);

		String returnMessage = "";
		
		//Formatar a mensagem de retorno com o problema original
		for(String part : problemDescription.split(";"))
		{
			returnMessage += part + "\n";
		}
		
		returnMessage += "\n";
		
		//Formatar a mensagem de retorno com a mensagem de retorno do GLPK
		returnMessage += GLPKSimplex.Solve();
		
		//Mandar a resposta da requisição POST
		response.getWriter().write(returnMessage);
	}
}
