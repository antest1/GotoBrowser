$.fn.rollover = function() {
   return this.each(function() {
      var src = $(this).attr('src');
      //すでに画像名に「_on.」が付いていた場合、ロールオーバー処理をしない
      if (src.match('_on.')) return;
	  if (src.match('_open.')) return;
      // ロールオーバー用の画像名を取得（_onを付加）
      var src_on = src.replace(/^(.+)(\.[a-z]+)$/, "$1_on$2");
      // 画像のプリロード（先読み込み）
      $('').attr('src', src_on);
      // ロールオーバー処理
      $(this).hover(
         function() { $(this).attr('src', src_on); },
         function() { $(this).attr('src', src); }
      );
   });
};


$(function() {
   $('.rollover a img').rollover();
   $('.bnr_request a img').rollover();
   window.addEventListener("message", function (e) {
	  //console.log("receive data from rollover:");
	  //console.log(e);
      var doc = document.getElementsByTagName("iframe")[0];
      doc.contentWindow.postMessage(e.data, "*")
   });
});