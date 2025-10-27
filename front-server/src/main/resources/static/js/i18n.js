// 다국어 지원 시스템
(function() {
    // 언어 번역 데이터
    const translations = {
        'KOREAN': {
            // 온보딩
            'onboarding.title': '다문화 가정을 위한\nAI 상담 서비스',
            'onboarding.subtitle': '언어와 문화의 장벽을 넘어, 필요한 도움을 쉽고 빠르게 받으세요',
            'onboarding.feature1.title': 'AI 상담',
            'onboarding.feature1.desc': '24시간 언제든지 AI와\n상담하세요',
            'onboarding.feature2.title': '문서 요약',
            'onboarding.feature2.desc': '복잡한 서류를 쉽게\n이해하세요',
            'onboarding.feature3.title': '다국어 지원',
            'onboarding.feature3.desc': '모국어로 편하게\n소통하세요',
            'onboarding.startButton': '시작하기',
            'onboarding.skip': '건너뛰기',

            // 네비게이션
            'nav.home': '홈',
            'nav.chat': '채팅',
            'nav.document': '문서',
            'nav.mypage': '마이페이지',

            // 홈
            'home.title': '다문화 가정을 위한\nAI 상담 서비스',
            'home.subtitle': '언어와 문화의 장벽을 넘어, 필요한 도움을 쉽고 빠르게 받으세요',
            'home.feature1.title': 'AI 상담',
            'home.feature1.desc': '24시간 언제든지 AI와\n상담하세요',
            'home.feature2.title': '문서 요약',
            'home.feature2.desc': '복잡한 서류를 쉽게\n이해하세요',
            'home.feature3.title': '다국어 지원',
            'home.feature3.desc': '모국어로 편하게\n소통하세요',
            'home.cta.title': '지금 바로 시작해보세요',
            'home.cta.chat': 'AI 상담 시작',
            'home.cta.document': '문서 요약',

            // 채팅
            'chat.newChat': '새로운 대화',
            'chat.title': '새로운 대화를 시작해보세요',
            'chat.welcomeTitle': '다문화 가정을 위한 AI 상담 서비스',
            'chat.welcomeDesc': '안녕하세요! 언어와 문화의 장벽 없이 필요한 도움을 받으실 수 있도록 돕는 AI 상담사입니다.\n편안하게 궁금하신 것을 물어보세요.',
            'chat.feature1.title': '24시간 상담',
            'chat.feature1.desc': '언제든지 대화를 시작하세요',
            'chat.feature2.title': '문서 도움',
            'chat.feature2.desc': '복잡한 서류를 쉽게 이해하세요',
            'chat.feature3.title': '다국어 지원',
            'chat.feature3.desc': '모국어로 편하게 소통하세요',
            'chat.feature4.title': '안전한 상담',
            'chat.feature4.desc': '개인정보가 보호됩니다',
            'chat.placeholder': '메시지를 입력하세요...',
            'chat.empty.title': '대화를 시작해보세요',
            'chat.empty.text': '궁금한 것을 물어보세요',
            'chat.noThreads': '아직 대화가 없습니다',
            'chat.newConversation': '새 대화',
            'chat.user': '사용자',
            'chat.documentSummary': '문서 요약',

            // 문서
            'document.title': '문서 요약',
            'document.subtitle': 'AI가 문서를 쉽고 빠르게 요약해드립니다',
            'document.infoTitle': '다문화 가정을 위한 문서 요약 서비스',
            'document.infoDesc': '복잡한 한국 서류, 공문서, 안내문이 어려우신가요?\nAI가 문서의 핵심 내용을 쉽고 간단하게 요약해드립니다.',
            'document.feature1.title': '자동 요약',
            'document.feature1.desc': '핵심만 빠르게 파악',
            'document.feature2.title': '다국어 지원',
            'document.feature2.desc': '모국어로 이해하기',
            'document.feature3.title': '빠른 처리',
            'document.feature3.desc': '몇 초 만에 완료',
            'document.feature4.title': '안전 보관',
            'document.feature4.desc': '언제든 다시 확인',
            'document.upload': '파일을 업로드하세요',
            'document.uploadHint': 'PDF, DOC, DOCX, TXT, JPG, PNG 파일을 지원합니다 (최대 10MB)',
            'document.myDocuments': '내 문서',
            'document.empty.title': '업로드한 문서가 없습니다',
            'document.empty.text': '문서를 업로드하면 AI가 자동으로 요약해드립니다',

            // 마이페이지
            'mypage.title': '마이페이지',
            'mypage.subtitle': '내 정보와 활동을 관리하세요',
            'mypage.infoTitle': '다문화 가정을 위한 종합 지원 서비스',
            'mypage.infoDesc': '언어와 문화의 차이로 어려움을 겪으시는 다문화 가정을 위해\nAI 상담, 문서 요약, 정보 안내 등 다양한 서비스를 제공합니다.',
            'mypage.editProfile': '프로필 수정',
            'mypage.settings': '설정',
            'mypage.language': '언어 설정',
            'mypage.notification': '알림 설정',
            'mypage.deleteData': '모든 데이터 삭제',
            'mypage.help': '도움말 & 정보',
            'mypage.guide': '사용 가이드',
            'mypage.about': '앱 정보',
            'mypage.logout': '로그아웃'
        },
        'ENGLISH': {
            // Onboarding
            'onboarding.title': 'AI Consulting Service\nfor Multicultural Families',
            'onboarding.subtitle': 'Get help easily beyond language and cultural barriers',
            'onboarding.feature1.title': 'AI Consultation',
            'onboarding.feature1.desc': 'Chat with AI\nanytime 24/7',
            'onboarding.feature2.title': 'Document Summary',
            'onboarding.feature2.desc': 'Understand complex\ndocuments easily',
            'onboarding.feature3.title': 'Multilingual',
            'onboarding.feature3.desc': 'Communicate in\nyour language',
            'onboarding.startButton': 'Get Started',
            'onboarding.skip': 'Skip',

            // Navigation
            'nav.home': 'Home',
            'nav.chat': 'Chat',
            'nav.document': 'Document',
            'nav.mypage': 'My Page',

            // Home
            'home.title': 'AI Consulting Service\nfor Multicultural Families',
            'home.subtitle': 'Get help easily beyond language and cultural barriers',
            'home.feature1.title': 'AI Consultation',
            'home.feature1.desc': 'Chat with AI\nanytime 24/7',
            'home.feature2.title': 'Document Summary',
            'home.feature2.desc': 'Understand complex\ndocuments easily',
            'home.feature3.title': 'Multilingual',
            'home.feature3.desc': 'Communicate in\nyour language',
            'home.cta.title': 'Get Started Now',
            'home.cta.chat': 'Start AI Chat',
            'home.cta.document': 'Summarize Document',

            // Chat
            'chat.newChat': 'New Chat',
            'chat.title': 'Start a new conversation',
            'chat.welcomeTitle': 'AI Consulting for Multicultural Families',
            'chat.welcomeDesc': 'Hello! I\'m an AI consultant to help you without language and cultural barriers.\nFeel free to ask me anything.',
            'chat.feature1.title': '24/7 Available',
            'chat.feature1.desc': 'Start chatting anytime',
            'chat.feature2.title': 'Document Help',
            'chat.feature2.desc': 'Understand complex documents',
            'chat.feature3.title': 'Multilingual',
            'chat.feature3.desc': 'Communicate in your language',
            'chat.feature4.title': 'Secure',
            'chat.feature4.desc': 'Your privacy is protected',
            'chat.placeholder': 'Type a message...',
            'chat.empty.title': 'Start a conversation',
            'chat.empty.text': 'Ask me anything',
            'chat.noThreads': 'No conversations yet',
            'chat.newConversation': 'New Chat',
            'chat.user': 'User',
            'chat.documentSummary': 'Document Summary',

            // Document
            'document.title': 'Document Summary',
            'document.subtitle': 'AI summarizes documents quickly and easily',
            'document.infoTitle': 'Document Summary for Multicultural Families',
            'document.infoDesc': 'Having trouble with Korean documents?\nAI summarizes key information simply and easily.',
            'document.feature1.title': 'Auto Summary',
            'document.feature1.desc': 'Identify key points',
            'document.feature2.title': 'Multilingual',
            'document.feature2.desc': 'Understand in your language',
            'document.feature3.title': 'Fast Processing',
            'document.feature3.desc': 'Done in seconds',
            'document.feature4.title': 'Safe Storage',
            'document.feature4.desc': 'Review anytime',
            'document.upload': 'Upload a file',
            'document.uploadHint': 'Supports PDF, DOC, DOCX, TXT, JPG, PNG (Max 10MB)',
            'document.myDocuments': 'My Documents',
            'document.empty.title': 'No uploaded documents',
            'document.empty.text': 'AI will automatically summarize uploaded documents',

            // My Page
            'mypage.title': 'My Page',
            'mypage.subtitle': 'Manage your information and activities',
            'mypage.infoTitle': 'Comprehensive Support for Multicultural Families',
            'mypage.infoDesc': 'For multicultural families facing language and cultural challenges,\nwe provide AI consultation, document summary, and information services.',
            'mypage.editProfile': 'Edit Profile',
            'mypage.settings': 'Settings',
            'mypage.language': 'Language',
            'mypage.notification': 'Notifications',
            'mypage.deleteData': 'Delete All Data',
            'mypage.help': 'Help & Info',
            'mypage.guide': 'User Guide',
            'mypage.about': 'About',
            'mypage.logout': 'Logout'
        },
        'VIETNAMESE': {
            // Onboarding
            'onboarding.title': 'Dịch vụ tư vấn AI\ncho gia đình đa văn hóa',
            'onboarding.subtitle': 'Nhận trợ giúp dễ dàng vượt qua rào cản ngôn ngữ và văn hóa',
            'onboarding.feature1.title': 'Tư vấn AI',
            'onboarding.feature1.desc': 'Trò chuyện với AI\nbất cứ lúc nào',
            'onboarding.feature2.title': 'Tóm tắt tài liệu',
            'onboarding.feature2.desc': 'Hiểu tài liệu phức tạp\nmột cách dễ dàng',
            'onboarding.feature3.title': 'Đa ngôn ngữ',
            'onboarding.feature3.desc': 'Giao tiếp bằng\ntiếng mẹ đẻ',
            'onboarding.startButton': 'Bắt đầu',
            'onboarding.skip': 'Bỏ qua',

            // Navigation
            'nav.home': 'Trang chủ',
            'nav.chat': 'Trò chuyện',
            'nav.document': 'Tài liệu',
            'nav.mypage': 'Trang của tôi',

            // Home
            'home.title': 'Dịch vụ tư vấn AI\ncho gia đình đa văn hóa',
            'home.subtitle': 'Nhận trợ giúp dễ dàng vượt qua rào cản ngôn ngữ và văn hóa',
            'home.feature1.title': 'Tư vấn AI',
            'home.feature1.desc': 'Trò chuyện với AI\nbất cứ lúc nào',
            'home.feature2.title': 'Tóm tắt tài liệu',
            'home.feature2.desc': 'Hiểu tài liệu phức tạp\nmột cách dễ dàng',
            'home.feature3.title': 'Đa ngôn ngữ',
            'home.feature3.desc': 'Giao tiếp bằng\ntiếng mẹ đẻ',
            'home.cta.title': 'Bắt đầu ngay bây giờ',
            'home.cta.chat': 'Bắt đầu trò chuyện AI',
            'home.cta.document': 'Tóm tắt tài liệu',

            // Chat
            'chat.newChat': 'Cuộc trò chuyện mới',
            'chat.title': 'Bắt đầu cuộc trò chuyện mới',
            'chat.welcomeTitle': 'Tư vấn AI cho gia đình đa văn hóa',
            'chat.welcomeDesc': 'Xin chào! Tôi là AI tư vấn giúp bạn vượt qua rào cản ngôn ngữ và văn hóa.\nHãy thoải mái hỏi tôi bất cứ điều gì.',
            'chat.feature1.title': 'Hỗ trợ 24/7',
            'chat.feature1.desc': 'Bắt đầu trò chuyện bất cứ lúc nào',
            'chat.feature2.title': 'Hỗ trợ tài liệu',
            'chat.feature2.desc': 'Hiểu tài liệu phức tạp',
            'chat.feature3.title': 'Đa ngôn ngữ',
            'chat.feature3.desc': 'Giao tiếp bằng ngôn ngữ của bạn',
            'chat.feature4.title': 'An toàn',
            'chat.feature4.desc': 'Quyền riêng tư được bảo vệ',
            'chat.placeholder': 'Nhập tin nhắn...',
            'chat.empty.title': 'Bắt đầu cuộc trò chuyện',
            'chat.empty.text': 'Hỏi tôi bất cứ điều gì',
            'chat.noThreads': 'Chưa có cuộc trò chuyện nào',
            'chat.newConversation': 'Cuộc trò chuyện mới',
            'chat.user': 'Người dùng',
            'chat.documentSummary': 'Tóm tắt tài liệu',

            // Document
            'document.title': 'Tóm tắt tài liệu',
            'document.subtitle': 'AI tóm tắt tài liệu nhanh chóng và dễ dàng',
            'document.infoTitle': 'Tóm tắt tài liệu cho gia đình đa văn hóa',
            'document.infoDesc': 'Gặp khó khăn với tài liệu tiếng Hàn?\nAI tóm tắt thông tin chính một cách đơn giản và dễ hiểu.',
            'document.feature1.title': 'Tóm tắt tự động',
            'document.feature1.desc': 'Xác định điểm chính',
            'document.feature2.title': 'Đa ngôn ngữ',
            'document.feature2.desc': 'Hiểu bằng ngôn ngữ của bạn',
            'document.feature3.title': 'Xử lý nhanh',
            'document.feature3.desc': 'Hoàn thành trong vài giây',
            'document.feature4.title': 'Lưu trữ an toàn',
            'document.feature4.desc': 'Xem lại bất cứ lúc nào',
            'document.upload': 'Tải lên tệp',
            'document.uploadHint': 'Hỗ trợ PDF, DOC, DOCX, TXT, JPG, PNG (Tối đa 10MB)',
            'document.myDocuments': 'Tài liệu của tôi',
            'document.empty.title': 'Không có tài liệu đã tải lên',
            'document.empty.text': 'AI sẽ tự động tóm tắt các tài liệu đã tải lên',

            // My Page
            'mypage.title': 'Trang của tôi',
            'mypage.subtitle': 'Quản lý thông tin và hoạt động của bạn',
            'mypage.infoTitle': 'Hỗ trợ toàn diện cho gia đình đa văn hóa',
            'mypage.infoDesc': 'Cho các gia đình đa văn hóa đối mặt với thách thức ngôn ngữ và văn hóa,\nchúng tôi cung cấp tư vấn AI, tóm tắt tài liệu và dịch vụ thông tin.',
            'mypage.editProfile': 'Chỉnh sửa hồ sơ',
            'mypage.settings': 'Cài đặt',
            'mypage.language': 'Ngôn ngữ',
            'mypage.notification': 'Thông báo',
            'mypage.deleteData': 'Xóa tất cả dữ liệu',
            'mypage.help': 'Trợ giúp & Thông tin',
            'mypage.guide': 'Hướng dẫn sử dụng',
            'mypage.about': 'Giới thiệu',
            'mypage.logout': 'Đăng xuất'
        },
        'CHINESE': {
            // Onboarding
            'onboarding.title': '多元文化家庭\nAI咨询服务',
            'onboarding.subtitle': '轻松跨越语言和文化障碍，获取所需帮助',
            'onboarding.feature1.title': 'AI咨询',
            'onboarding.feature1.desc': '24小时\n随时咨询',
            'onboarding.feature2.title': '文档摘要',
            'onboarding.feature2.desc': '轻松理解\n复杂文档',
            'onboarding.feature3.title': '多语言',
            'onboarding.feature3.desc': '用母语\n轻松交流',
            'onboarding.startButton': '开始使用',
            'onboarding.skip': '跳过',

            // Navigation
            'nav.home': '首页',
            'nav.chat': '聊天',
            'nav.document': '文档',
            'nav.mypage': '我的页面',

            // Home
            'home.title': '多元文化家庭\nAI咨询服务',
            'home.subtitle': '轻松跨越语言和文化障碍，获取所需帮助',
            'home.feature1.title': 'AI咨询',
            'home.feature1.desc': '24小时\n随时咨询',
            'home.feature2.title': '文档摘要',
            'home.feature2.desc': '轻松理解\n复杂文档',
            'home.feature3.title': '多语言',
            'home.feature3.desc': '用母语\n轻松交流',
            'home.cta.title': '立即开始',
            'home.cta.chat': '开始AI咨询',
            'home.cta.document': '文档摘要',

            // Chat
            'chat.newChat': '新对话',
            'chat.title': '开始新对话',
            'chat.welcomeTitle': '多元文化家庭AI咨询',
            'chat.welcomeDesc': '您好！我是AI咨询顾问，帮助您跨越语言和文化障碍。\n请随时向我提问。',
            'chat.feature1.title': '全天候服务',
            'chat.feature1.desc': '随时开始聊天',
            'chat.feature2.title': '文档帮助',
            'chat.feature2.desc': '理解复杂文档',
            'chat.feature3.title': '多语言',
            'chat.feature3.desc': '用您的语言交流',
            'chat.feature4.title': '安全',
            'chat.feature4.desc': '隐私受保护',
            'chat.placeholder': '输入消息...',
            'chat.empty.title': '开始对话',
            'chat.empty.text': '问我任何问题',
            'chat.noThreads': '暂无对话',
            'chat.newConversation': '新对话',
            'chat.user': '用户',
            'chat.documentSummary': '文档摘要',

            // Document
            'document.title': '文档摘要',
            'document.subtitle': 'AI快速轻松地总结文档',
            'document.infoTitle': '多元文化家庭文档摘要',
            'document.infoDesc': '韩语文档有困难？\nAI简单易懂地总结关键信息。',
            'document.feature1.title': '自动摘要',
            'document.feature1.desc': '快速识别要点',
            'document.feature2.title': '多语言',
            'document.feature2.desc': '用您的语言理解',
            'document.feature3.title': '快速处理',
            'document.feature3.desc': '几秒内完成',
            'document.feature4.title': '安全存储',
            'document.feature4.desc': '随时查看',
            'document.upload': '上传文件',
            'document.uploadHint': '支持PDF、DOC、DOCX、TXT、JPG、PNG（最大10MB）',
            'document.myDocuments': '我的文档',
            'document.empty.title': '没有上传的文档',
            'document.empty.text': 'AI将自动摘要上传的文档',

            // My Page
            'mypage.title': '我的页面',
            'mypage.subtitle': '管理您的信息和活动',
            'mypage.infoTitle': '多元文化家庭综合支持',
            'mypage.infoDesc': '对于面临语言和文化挑战的多元文化家庭，\n我们提供AI咨询、文档摘要和信息服务。',
            'mypage.editProfile': '编辑个人资料',
            'mypage.settings': '设置',
            'mypage.language': '语言',
            'mypage.notification': '通知',
            'mypage.deleteData': '删除所有数据',
            'mypage.help': '帮助与信息',
            'mypage.guide': '使用指南',
            'mypage.about': '关于',
            'mypage.logout': '退出登录'
        },
        'JAPANESE': {
            // Onboarding
            'onboarding.title': '多文化家族のための\nAI相談サービス',
            'onboarding.subtitle': '言語と文化の壁を越えて、必要な支援を簡単に受けられます',
            'onboarding.feature1.title': 'AI相談',
            'onboarding.feature1.desc': '24時間いつでも\nAIと相談',
            'onboarding.feature2.title': '書類要約',
            'onboarding.feature2.desc': '複雑な書類を\n簡単に理解',
            'onboarding.feature3.title': '多言語対応',
            'onboarding.feature3.desc': '母国語で\n気軽にコミュニケーション',
            'onboarding.startButton': '始める',
            'onboarding.skip': 'スキップ',

            // Navigation
            'nav.home': 'ホーム',
            'nav.chat': 'チャット',
            'nav.document': '書類',
            'nav.mypage': 'マイページ',

            // Home
            'home.title': '多文化家族のための\nAI相談サービス',
            'home.subtitle': '言語と文化の壁を越えて、必要な支援を簡単に受けられます',
            'home.feature1.title': 'AI相談',
            'home.feature1.desc': '24時間いつでも\nAIと相談',
            'home.feature2.title': '書類要約',
            'home.feature2.desc': '複雑な書類を\n簡単に理解',
            'home.feature3.title': '多言語対応',
            'home.feature3.desc': '母国語で\n気軽にコミュニケーション',
            'home.cta.title': '今すぐ始める',
            'home.cta.chat': 'AI相談を始める',
            'home.cta.document': '書類要約',

            // Chat
            'chat.newChat': '新しい会話',
            'chat.title': '新しい会話を始める',
            'chat.welcomeTitle': '多文化家族のためのAI相談',
            'chat.welcomeDesc': 'こんにちは！言語と文化の壁なく必要な支援を受けられるよう、お手伝いするAI相談員です。\nお気軽にご質問ください。',
            'chat.feature1.title': '24時間対応',
            'chat.feature1.desc': 'いつでもチャット開始',
            'chat.feature2.title': '書類サポート',
            'chat.feature2.desc': '複雑な書類を理解',
            'chat.feature3.title': '多言語対応',
            'chat.feature3.desc': '母国語でコミュニケーション',
            'chat.feature4.title': '安全',
            'chat.feature4.desc': 'プライバシーを保護',
            'chat.placeholder': 'メッセージを入力...',
            'chat.empty.title': '会話を始める',
            'chat.empty.text': '何でも聞いてください',
            'chat.noThreads': 'まだ会話がありません',
            'chat.newConversation': '新しい会話',
            'chat.user': 'ユーザー',
            'chat.documentSummary': '書類要約',

            // Document
            'document.title': '書類要約',
            'document.subtitle': 'AIが書類を迅速かつ簡単に要約',
            'document.infoTitle': '多文化家族のための書類要約',
            'document.infoDesc': '韓国語の書類でお困りですか？\nAIが重要な情報を簡単にわかりやすく要約します。',
            'document.feature1.title': '自動要約',
            'document.feature1.desc': 'ポイントを素早く把握',
            'document.feature2.title': '多言語対応',
            'document.feature2.desc': '母国語で理解',
            'document.feature3.title': '高速処理',
            'document.feature3.desc': '数秒で完了',
            'document.feature4.title': '安全保管',
            'document.feature4.desc': 'いつでも確認',
            'document.upload': 'ファイルをアップロード',
            'document.uploadHint': 'PDF、DOC、DOCX、TXT、JPG、PNGに対応（最大10MB）',
            'document.myDocuments': 'マイ書類',
            'document.empty.title': 'アップロードされた書類がありません',
            'document.empty.text': 'AIがアップロードされた書類を自動的に要約します',

            // My Page
            'mypage.title': 'マイページ',
            'mypage.subtitle': '情報と活動を管理',
            'mypage.infoTitle': '多文化家族への総合サポート',
            'mypage.infoDesc': '言語と文化の課題に直面している多文化家族のために、\nAI相談、書類要約、情報サービスを提供します。',
            'mypage.editProfile': 'プロフィール編集',
            'mypage.settings': '設定',
            'mypage.language': '言語',
            'mypage.notification': '通知',
            'mypage.deleteData': 'すべてのデータを削除',
            'mypage.help': 'ヘルプと情報',
            'mypage.guide': '使用ガイド',
            'mypage.about': 'アプリについて',
            'mypage.logout': 'ログアウト'
        }
    };

    // 현재 언어 가져오기
    function getCurrentLanguage() {
        return localStorage.getItem('language') || '한국어';
    }

    // 언어 설정
    function setLanguage(lang) {
        localStorage.setItem('language', lang);
    }

    // 번역 가져오기
    function translate(key, lang) {
        lang = lang || getCurrentLanguage();
        const langCode = getLanguageCode(lang);
        return translations[langCode]?.[key] || translations['KOREAN'][key] || key;
    }

    // 언어 이름을 enum 이름으로 변환
    function getLanguageCode(langName) {
        const langMap = {
            '한국어': 'KOREAN',
            'English': 'ENGLISH',
            '中文': 'CHINESE',
            'Tiếng Việt': 'VIETNAMESE',
            '日本語': 'JAPANESE'
        };
        return langMap[langName] || 'KOREAN';
    }

    // 페이지의 모든 data-i18n 요소 업데이트
    function updatePageLanguage() {
        const elements = document.querySelectorAll('[data-i18n]');
        elements.forEach(element => {
            const key = element.getAttribute('data-i18n');
            const text = translate(key);

            // 줄바꿈 처리
            if (text.includes('\n')) {
                element.innerHTML = text.replace(/\n/g, '<br>');
            } else {
                element.textContent = text;
            }
        });

        // placeholder 업데이트
        const placeholderElements = document.querySelectorAll('[data-i18n-placeholder]');
        placeholderElements.forEach(element => {
            const key = element.getAttribute('data-i18n-placeholder');
            const text = translate(key);
            element.placeholder = text;
        });
    }

    // 전역으로 노출
    window.i18n = {
        translate: translate,
        getCurrentLanguage: getCurrentLanguage,
        setLanguage: setLanguage,
        updatePageLanguage: updatePageLanguage,
        getLanguageCode: getLanguageCode
    };
})();
