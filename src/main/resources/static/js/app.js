// Melhoria progressiva: o sistema funciona sem JavaScript; aqui só entram conveniências.
(function () {
  'use strict';

  // Evita envio duplo e mostra que o formulário está sendo processado.
  document.addEventListener('submit', function (e) {
    var form = e.target;
    if (!(form instanceof HTMLFormElement)) return;
    var botao = form.querySelector('button[type="submit"][data-ocupado]');
    if (!botao || botao.getAttribute('aria-busy') === 'true') return;
    botao.dataset.rotulo = botao.textContent;
    botao.setAttribute('aria-busy', 'true');
    botao.textContent = botao.dataset.ocupado;
  });

  // Ao voltar pelo histórico do navegador, restaura os botões.
  window.addEventListener('pageshow', function () {
    document.querySelectorAll('button[aria-busy="true"]').forEach(function (b) {
      b.removeAttribute('aria-busy');
      if (b.dataset.rotulo) b.textContent = b.dataset.rotulo;
    });
  });

  // Avisos de sucesso somem sozinhos.
  document.querySelectorAll('[data-aviso]').forEach(function (a) {
    setTimeout(function () { a.remove(); }, 6000);
  });
})();
