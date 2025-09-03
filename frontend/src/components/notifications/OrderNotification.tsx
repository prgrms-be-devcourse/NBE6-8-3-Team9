// "use client";
//
// import { useEffect } from "react";
// import SockJS from "sockjs-client";
// import { Client } from "@stomp/stompjs";
//
// export default function OrderNotification({ userId }: { userId: number }) {
//     useEffect(() => {
//         // 여러 엔드포인트 시도 (local → cloudfront)
//         const endpoints = [
//             process.env.NODE_ENV === "production"
//                 ? "https://d64t5u28gt0rl.cloudfront.net/ws"
//                 : "http://localhost:8080/ws",
//         ];
//
//         let stompClient: Client | null = null;
//         let socket: any = null;
//
//         const connect = (index: number) => {
//             if (index >= endpoints.length) {
//                 console.error("모든 WebSocket 엔드포인트 연결 실패");
//                 return;
//             }
//
//             const url = endpoints[index];
//             console.log(`${url} 연결 시도 중...`);
//             socket = new SockJS(url);
//
//             stompClient = new Client({
//                 webSocketFactory: () => socket,
//                 debug: (str) => console.log(str),
//                 onConnect: () => {
//                     console.log(`✅ WebSocket 연결 성공: ${url}`);
//                     stompClient?.subscribe(`/topic/orders.${userId}`, (message) => {
//                         const notification = JSON.parse(message.body);
//                         console.log("주문 알림:", notification);
//                         alert(`주문 알림: ${notification.message}`);
//                     });
//                 },
//                 onStompError: (frame) => {
//                     console.error("STOMP 에러:", frame);
//                 },
//                 onWebSocketClose: () => {
//                     console.warn(`연결 종료됨: ${url}, 다음 엔드포인트 시도`);
//                     connect(index + 1); // 다음 엔드포인트 시도
//                 },
//             });
//
//             stompClient.activate();
//         };
//
//         connect(0);
//
//         return () => {
//             stompClient?.deactivate();
//             socket?.close();
//         };
//     }, [userId]);
//
//     return <div>실시간 주문 알림 수신 중...</div>;
// }
