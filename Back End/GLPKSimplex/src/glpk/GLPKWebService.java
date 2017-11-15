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

	//Vari�veis locais para utiliza��o do GLPK
	private static glp_prob Problem;
	private static glp_smcp Params;
	private static SWIGTYPE_p_int Indexes;
	private static SWIGTYPE_p_double Values;
	
	//M�todo para cria��o do problema no GLPK 
	public static void InitializeProblem(String problemName, int constraintsCount)
	{
		//Cria um novo problema
		Problem = GLPK.glp_create_prob();
		
		//Atribui um nome ao problema
		GLPK.glp_set_prob_name(Problem, problemName);
		
		//Adiciona o n�mero de linhas(restri��es) ao problema
		GLPK.glp_add_rows(Problem, constraintsCount);
	}

	//M�todo para atribui��o da fun��o objetiva para o problema criado no GLPK
	public static void SetObjectiveFunction(String ofRepresentation, int variableCount, boolean isMax)
	{
		//Adiciona o n�mero de colunas(vari�veis) ao problema
		GLPK.glp_add_cols(Problem, variableCount);
		
		//Atribui o nome da fun��o objetiva no problema
		GLPK.glp_set_obj_name(Problem, "FO(Z)");
		
		//Atribui o tipo da fun��o objetiva (Maximiza��o ou Miniza��o) no problema
		GLPK.glp_set_obj_dir(Problem, (isMax) ? GLPKConstants.GLP_MAX : GLPKConstants.GLP_MIN);

		//Divide a string da fun��o objetiva por espa�os(" ")
		String[] zSplit = ofRepresentation.split(" ");

		int varCounter = 1;

		for(String subZ : zSplit)
		{
			//Divide cada divis�o da FO em ponto e v�rgula(";")
			String[] member = subZ.split(";");

			//Extrai o valor da string
			double value = Double.parseDouble(member[0]);

			//Atribui o nome da vari�vel a coluna do problema
			GLPK.glp_set_col_name(Problem, varCounter, member[1]);
			
			//Define que todas as vari�veis t�m de ser maiores ou iguais a 0
			GLPK.glp_set_col_bnds(Problem, varCounter, GLPKConstants.GLP_LO, 0, 0);

			//Determina se a vari�vel � double ou int pela presen�a ou n�o do ponto
			boolean isInt = !member[0].contains(".");

			//Atribui o tipo da vari�vel (Double ou int)
			GLPK.glp_set_col_kind(Problem, varCounter, (isInt) ? GLPKConstants.GLP_IV : GLPKConstants.GLP_CV);
			
			//Atribui o coeficiente da vari�vel na FO
			GLPK.glp_set_obj_coef(Problem, varCounter, value);
			
			++varCounter;
		}
	}

	//M�todo para atribui��o das restri��es para o problema criado no GLPK
	public static void AddConstraints(Map<String, String> constraints)
	{
		//Pega quantas vari�veis existem no problema
		int size = GLPK.glp_get_num_cols(Problem);

		//Aloca mem�ria pois o GLPK � uma library nativa (C/C++)
		Indexes = GLPK.new_intArray(size);
		Values = GLPK.new_doubleArray(size);

		int constraintCount = 1;
		int varCount = 1;
		
		//Para cada restri��o (string)
		for(Map.Entry<String, String> entry : constraints.entrySet())
		{
			//Atribui o nome da linha no problema ao "nome" da restri��o
			GLPK.glp_set_row_name(Problem, constraintCount, entry.getKey());
			
			for(int i = 1; i <= size; i++)
			{
				GLPK.intArray_setitem(Indexes, i, i);
			}

			//Pega o valor em string da restri��o
			String entryValue = entry.getValue();

			//Divide a restri��o por espa�os (" ")
			String[] members = entryValue.split(" ");

			varCount = 1;
			
			//Para cada parte separada por espa�o (" ")
			for(String member : members)
			{
				//Se contem ";" indicando que � um coeficiente com vari�vel
				if(member.contains(";"))
				{
					//Divide a parte por ponto e v�rgula (";")
					String[] parts = member.split(";");

					//Pega o valor do coeficiente
					double partValue = Double.parseDouble(parts[0]);

					//Atribui o coeficiente ao item (variavel) correto
					GLPK.doubleArray_setitem(Values, varCount, partValue);
					
					++varCount;
				}
				//Se for ">=" ou "<=" "continuar" a itera��o
				else if(member.contains(">=") || member.contains("<="))
				{
					continue;
				}
				else
				{
					//Define um limite inferior e superior como zero
					double loBound = 0, upBound = 0;

					//Se for maior ou igual define o limite inferior, caso contr�rio o inferior
					if(entryValue.contains(">="))
					{
						loBound = Double.parseDouble(member);
					}
					else
					{
						upBound = Double.parseDouble(member);
					}

					//Atribui os limites a aquela restri��o dentro do problema
					GLPK.glp_set_row_bnds(Problem, constraintCount, (loBound != 0) ? GLPKConstants.GLP_LO : GLPKConstants.GLP_UP, loBound, upBound);
				}
			}

			//Atribui a restri��o para o problema em seu lugar correto na matriz
			GLPK.glp_set_mat_row(Problem, constraintCount, size, Indexes, Values);
			
			++constraintCount;
		}

		//Libera a mem�ria alocada
		GLPK.delete_intArray(Indexes);
		GLPK.delete_doubleArray(Values);
	}

	//M�todo apra resolver o problema
	public static String Solve()
	{
		try
		{
			//Instancia par�metros utilizados pelo resolvedor de Simplex do GLPK
			Params = new glp_smcp();
			GLPK.glp_init_smcp(Params);
			String returnMessage = "";
	
			//Verifica se o problema foi ou n�o resolvido
			if (GLPK.glp_simplex(Problem, Params) == 0) 
			{
				//Caso tenho sido retornar a solu��o "completa"
				returnMessage = GetPrintableSolution();
			} 
			else 
			{
				returnMessage =  "O problema nao pode ser resolvido.";
			}
			
			//Deletar o problema do GLPK, liberando mem�ria...
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

		//Pegar o c�digo do status da solu��o do problema
		int problemStatus = GLPK.glp_get_status(Problem);
		
		//Traduzir o c�digo do status
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
		
		//Recuperar o valor para fun��o objetiva
		name = GLPK.glp_get_obj_name(Problem);
		val = GLPK.glp_get_obj_val(Problem);
		
		message += name + " = " + val + "\n";
		
		n = GLPK.glp_get_num_cols(Problem);
		
		//Recuperar o valor para as vari�veis do problema
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
		//Recuperar os valores dos par�metros da requisi��o POST
		String type = request.getParameter("Type");
		String of = request.getParameter("FO(Z)");
		String problemDescription = request.getParameter("Description");
		int varCount = Integer.parseInt(request.getParameter("VariableCount"));
		int restrictionCount = Integer.parseInt(request.getParameter("RestrictionCount"));

		Map<String, String[]> parameterMap = new HashMap<String, String[]>(request.getParameterMap());

		//Remover os par�metros j� recuperados
		parameterMap.remove("Type");
		parameterMap.remove("VariableCount");
		parameterMap.remove("RestrictionCount");
		parameterMap.remove("FO(Z)");
		parameterMap.remove("Description");

		Map<String,String> constraints = new HashMap<String, String>();

		//Para cada par�metro restante adicion� lo em um mapeamento composto do seu nome e seu valor
		for(String key : parameterMap.keySet())
		{
			constraints.put(key, request.getParameter(key));
		}

		//Inicializar o problema no GLPK
		GLPKSimplex.InitializeProblem("Problema de Mix de Producao", restrictionCount);
		
		//Definir a FO no GLPK
		GLPKSimplex.SetObjectiveFunction(of, varCount, type.equals("MAX"));
		
		//Definir as restri��es no GLPK
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
		
		//Mandar a resposta da requisi��o POST
		response.getWriter().write(returnMessage);
	}
}
